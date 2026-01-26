
import http.server
import socketserver
import json
import re
from urllib.parse import urlparse, parse_qs

PORT = 5055

# --- Mock Data ---

FAKE_MOVIES = [
    {
        "id": 1001,
        "mediaType": "movie",
        "title": "Neon Horizons",
        "overview": "In a futuristic city where technology and humanity collide...",
        "posterPath": "/poster/movie_1001.jpg",
        "backdropPath": "/backdrop/movie_1001.jpg",
        "releaseDate": "2025-06-15",
        "voteAverage": 8.7,
        "mediaInfo": {"status": 5, "available": True}
    },
    {
        "id": 1002,
        "mediaType": "movie",
        "title": "The Last Cartographer",
        "overview": "An aging mapmaker discovers...",
        "posterPath": "/poster/movie_1002.jpg",
        "releaseDate": "2024-06-15",
        "voteAverage": 8.2,
        "mediaInfo": {"status": 5, "available": True}
    }
]

FAKE_USER = {
    "id": 1,
    "email": "admin@example.com",
    "displayName": "Admin User",
    "avatar": "/avatar/admin.jpg",
    "requestCount": 18,
    "permissions": 2
}

def get_search_results(page=1, query=""):
    # Simplified mock response
    results = FAKE_MOVIES
    return {
        "page": page,
        "totalPages": 1,
        "totalResults": len(results),
        "results": results
    }

class MockHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path
        
        response_data = {}
        status_code = 200

        print(f"GET {path}")

        if path == "/api/v1/status":
            response_data = {
                "version": "1.33.2",
                "initialized": True,
                "applicationUrl": "http://10.0.2.2:5055"
            }
        
        elif path == "/api/v1/auth/me" or path == "/api/v1/user":
            response_data = FAKE_USER
            
        elif path == "/api/v1/user/quota":
             response_data = {"movie": {"limit": 10, "remaining": 7, "days": 7}, "tv": {"limit": 10, "remaining": 7, "days": 7}}
             
        elif path == "/api/v1/user/stats":
             response_data = {"totalRequests": 32, "approvedRequests": 24, "declinedRequests": 2, "pendingRequests": 4, "availableRequests": 2}

        elif path.startswith("/api/v1/discover/trending") or path.startswith("/api/v1/discover/movies"):
             response_data = get_search_results()

        elif path.startswith("/api/v1/search"):
             response_data = get_search_results()
             
        elif path == "/api/v1/request":
             response_data = {"pageInfo": {"pages": 1, "pageSize": 20, "results": 0, "page": 1}, "results": []}

        else:
            # Fallback 404
            if not path.startswith("/poster") and not path.startswith("/backdrop"): 
                status_code = 404
                response_data = {"message": "Not Found"}

        if status_code == 200:
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps(response_data).encode('utf-8'))
        elif path.startswith("/poster") or path.startswith("/backdrop"):
             self.send_response(200)
             self.send_header('Content-type', 'image/png')
             self.end_headers()
             try:
                 with open("website/screenshots/issues.png", "rb") as f:
                     self.wfile.write(f.read())
             except Exception as e:
                 print(f"Error serving image: {e}")
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        print(f"POST {path}")
        
        response_data = {}
        
        if path == "/api/v1/auth/plex":
            response_data = FAKE_USER
            # Return Auth response structure
            response_data = {
                "apiKey": "test-key",
                "userId": 1,
                "user": FAKE_USER
            }
            
        elif path.startswith("/api/v1/request"):
             response_data = {"id": 1, "status": 1, "media": {"status": 1}}

        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(response_data).encode('utf-8'))

# Allow reuse address
socketserver.TCPServer.allow_reuse_address = True

if __name__ == "__main__":
    with socketserver.TCPServer(("0.0.0.0", PORT), MockHandler) as httpd:
        print(f"Serving mock at port {PORT}")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            pass
