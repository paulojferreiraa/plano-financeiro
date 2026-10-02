module.exports = async function handler(request, response) {
    const backendUrl = process.env.BACKEND_URL;

    if (!backendUrl) {
        return response.status(503).json({
            message: "A API ainda não foi conectada. Configure BACKEND_URL no Vercel."
        });
    }

    try {
        const incomingUrl = new URL(request.url, "https://vercel.invalid");
        if (!incomingUrl.pathname.startsWith("/api/")) {
            return response.status(404).end();
        }

        const backend = new URL(backendUrl);
        const target = new URL(`${incomingUrl.pathname}${incomingUrl.search}`, backend.origin);
        const headers = {};

        for (const name of ["accept", "content-type", "cookie", "x-xsrf-token"]) {
            const value = request.headers[name];
            if (value) headers[name] = value;
        }

        const options = {
            method: request.method,
            headers,
            redirect: "manual",
            signal: AbortSignal.timeout(8000)
        };

        if (request.method !== "GET" && request.method !== "HEAD" && request.body !== undefined) {
            options.body = typeof request.body === "string"
                ? request.body
                : JSON.stringify(request.body);
        }

        const upstream = await fetch(target, options);
        response.status(upstream.status);
        response.setHeader("Cache-Control", "no-store");

        const contentType = upstream.headers.get("content-type");
        if (contentType) response.setHeader("Content-Type", contentType);

        const cookies = upstream.headers.getSetCookie?.() || [];
        if (cookies.length) response.setHeader("Set-Cookie", cookies);

        if (upstream.status === 204 || upstream.status === 304) {
            return response.end();
        }

        return response.end(Buffer.from(await upstream.arrayBuffer()));
    } catch (error) {
        console.error("Falha ao conectar ao backend financeiro:", error);
        return response.status(502).json({
            message: "Não foi possível conectar ao servidor financeiro."
        });
    }
};
