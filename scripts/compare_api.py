import requests
import json
import re

def fetch_and_extract_swagger(url):
    print(f"Fetching {url}...")
    try:
        response = requests.get(url)
        response.raise_for_status()
        content = response.text
        
        # Extract the JSON object assigned to "swaggerDoc"
        # Adjusted regex to capture the content of swaggerDoc more reliably
        # Looking for "swaggerDoc": { ... }, "customOptions"
        # Since it's nested JSON inside JS, it might be tricky with regex if it spans many lines.
        # But looking at previous output, it seems standard.
        
        # Strategy: Find start of "swaggerDoc": and count braces to find the end
        start_marker = '"swaggerDoc": '
        start_index = content.find(start_marker)
        if start_index == -1:
            print("Could not find swaggerDoc start")
            return None
        
        start_index += len(start_marker)
        
        # specific simple parser for valid JSON object structure
        brace_count = 0
        json_str = ""
        started = False
        
        for i in range(start_index, len(content)):
            char = content[i]
            if char == '{':
                brace_count += 1
                started = True
            elif char == '}':
                brace_count -= 1
            
            json_str += char
            
            if started and brace_count == 0:
                break
        
        return json.loads(json_str)

    except Exception as e:
        print(f"Error fetching/parsing {url}: {e}")
        return None

def compare_specs(overseerr, jellyseerr):
    print("\n--- API Comparison ---")
    
    # Compare Info
    print(f"Overseerr Version: {overseerr['info']['version']}")
    print(f"Jellyseerr Version: {jellyseerr['info']['version']}")
    
    # Compare Tags (Categories)
    o_tags = {t['name'] for t in overseerr['tags']}
    j_tags = {t['name'] for t in jellyseerr['tags']}
    
    new_tags = j_tags - o_tags
    missing_tags = o_tags - j_tags
    
    if new_tags:
        print(f"\nNew Categories (Tags) in Jellyseerr: {', '.join(new_tags)}")
    if missing_tags:
        print(f"Missing Categories (Tags) in Jellyseerr: {', '.join(missing_tags)}")
        
    # Compare Paths (Endpoints)
    o_paths = set(overseerr['paths'].keys())
    j_paths = set(jellyseerr['paths'].keys())
    
    new_endpoints = sorted(list(j_paths - o_paths))
    missing_endpoints = sorted(list(o_paths - j_paths))
    
    print(f"\nTotal Endpoints: Overseerr={len(o_paths)}, Jellyseerr={len(j_paths)}")
    
    if new_endpoints:
        print("\nNew Endpoints in Jellyseerr:")
        for ep in new_endpoints:
            methods = list(jellyseerr['paths'][ep].keys())
            print(f"  + {ep} ({', '.join(methods)})")
            
    if missing_endpoints:
        print("\nMissing Endpoints in Jellyseerr:")
        for ep in missing_endpoints:
            methods = list(overseerr['paths'][ep].keys())
            print(f"  - {ep} ({', '.join(methods)})")

    # Compare Methods within common paths
    common_paths = o_paths.intersection(j_paths)
    changed_methods = []
    
    for ep in common_paths:
        o_methods = set(overseerr['paths'][ep].keys())
        j_methods = set(jellyseerr['paths'][ep].keys())
        
        if o_methods != j_methods:
            changed_methods.append((ep, o_methods, j_methods))
            
    if changed_methods:
        print("\nChanged Methods on Existing Endpoints:")
        for ep, o_m, j_m in changed_methods:
            added = j_m - o_m
            removed = o_m - j_m
            changes = []
            if added: changes.append(f"Added: {', '.join(added)}")
            if removed: changes.append(f"Removed: {', '.join(removed)}")
            print(f"  * {ep}: {'; '.join(changes)}")

    # Compare Schemas (Data Models)
    if 'components' in overseerr and 'schemas' in overseerr['components']:
        o_schemas = set(overseerr['components']['schemas'].keys())
        j_schemas = set(jellyseerr['components']['schemas'].keys())
        
        new_schemas = j_schemas - o_schemas
        missing_schemas = o_schemas - j_schemas
        
        if new_schemas:
            print(f"\nNew Data Models (Schemas): {', '.join(sorted(new_schemas))}")
        if missing_schemas:
            print(f"Missing Data Models (Schemas): {', '.join(sorted(missing_schemas))}")

def main():
    overseerr_url = "http://localhost:5055/api-docs/swagger-ui-init.js"
    jellyseerr_url = "http://localhost:5056/api-docs/swagger-ui-init.js"
    
    overseerr_spec = fetch_and_extract_swagger(overseerr_url)
    jellyseerr_spec = fetch_and_extract_swagger(jellyseerr_url)
    
    if overseerr_spec and jellyseerr_spec:
        compare_specs(overseerr_spec, jellyseerr_spec)
    else:
        print("Failed to get specifications.")

if __name__ == "__main__":
    main()
