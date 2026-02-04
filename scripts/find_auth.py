import requests
import re

def find_auth_paths(url):
    print(f"Fetching {url}...")
    try:
        response = requests.get(url)
        response.raise_for_status()
        content = response.text
        
        # Look for keys in the paths object
        # Regex to find "/auth/..." inside quotes
        auth_paths = re.findall(r'"/auth/[^"]+"', content)
        
        print("Found auth paths:")
        for p in sorted(list(set(auth_paths))):
            print(p)
            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    find_auth_paths("http://localhost:5056/api-docs/swagger-ui-init.js")
