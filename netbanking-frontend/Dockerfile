# Lightweight Node.js container for Oracle JET Redwood Frontend
FROM node:20-alpine
WORKDIR /app

# Copy server and static web assets
COPY server.js ./
COPY src ./src

EXPOSE 8000

ENV PORT=8000
CMD ["node", "server.js"]
