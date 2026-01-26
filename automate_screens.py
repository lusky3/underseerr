import subprocess
import time
import re
import sys
import os

def adb(command):
    cmd = f"export PATH=$PATH:/home/cody/android-sdk/platform-tools && adb {command}"
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return result.stdout.strip()

def dump_ui():
    adb("shell uiautomator dump /sdcard/window_dump.xml")
    content = adb("shell cat /sdcard/window_dump.xml")
    return content

def find_bounds(xml, text_pattern):
    match = re.search(f'text="[^"]*{text_pattern}[^"]*".*?bounds="(\\[\\d+,\\d+\\]\\[\\d+,\\d+\\])"', xml, re.IGNORECASE)
    if not match:
        match = re.search(f'content-desc="[^"]*{text_pattern}[^"]*".*?bounds="(\\[\\d+,\\d+\\]\\[\\d+,\\d+\\])"', xml, re.IGNORECASE)
    
    if match:
        return match.group(1)
    return None

def parse_bounds(bounds_str):
    nums = re.findall(r'\d+', bounds_str)
    x1, y1, x2, y2 = map(int, nums)
    return (x1 + x2) // 2, (y1 + y2) // 2

def click_element_by_text(text):
    print(f"Looking for '{text}'...")
    xml = dump_ui()
    if not xml or "ERROR" in xml:
        print("Failed to dump UI")
        return False
        
    bounds = find_bounds(xml, text)
    if bounds:
        x, y = parse_bounds(bounds)
        print(f"Found '{text}' at {x},{y}. Clicking...")
        adb(f"shell input tap {x} {y}")
        return True
    print(f"Element '{text}' not found.")
    return False

def wait_for_text(text, timeout=30):
    print(f"Waiting for '{text}'...")
    start = time.time()
    while time.time() - start < timeout:
        xml = dump_ui()
        if text.lower() in xml.lower():
            return True
        time.sleep(2)
    print(f"Timeout waiting for '{text}'. Current screen text sample:")
    # Print a localized sample around the middle of the XML or just some unique text
    # Or just print whole XML if small enough, but it's huge.
    # Let's print visible text.
    visible_texts = re.findall(r'text="([^"]+)"', xml)
    print(visible_texts[:20]) # First 20 text items
    return False

def main():
    print("Starting automation...")
    
    # 1. Launch
    adb("shell monkey -p app.lusk.client.debug -c android.intent.category.LAUNCHER 1")
    time.sleep(8)
    
    # 2. Permission
    if wait_for_text("Allow", timeout=10):
        click_element_by_text("Allow")
        time.sleep(3)
    
    # 3. Config Screen
    if wait_for_text("Configure Server", timeout=15):
        print("On Config Screen")
        if not click_element_by_text("Mock Server"):
            print("Mock Server button not found (scrolling...)")
            adb("shell input swipe 540 1500 540 500 300")
            time.sleep(1)
            click_element_by_text("Mock Server")
        
        # Wait for validation to complete
        time.sleep(8)
    
    # 4. Auth Screen
    if wait_for_text("Sign in with Plex", timeout=20):
        print("On Auth Screen")
        if not click_element_by_text("Mock Auth"):
             print("Mock Auth button not found (scrolling...)")
             adb("shell input swipe 540 1500 540 500 300")
             time.sleep(1)
             click_element_by_text("Mock Auth")
        
        time.sleep(8)
    
    # 5. Home Capture
    if wait_for_text("Trending", timeout=20) or wait_for_text("Discover", timeout=5):
        print("On Home Screen - Capturing")
        time.sleep(5) 
        adb("shell screencap -p /data/local/tmp/01_home.png")
        adb("pull /data/local/tmp/01_home.png screenshots/01_home.png")
        
        # 6. Details
        # Try to find a movie title from mock data
        if click_element_by_text("Neon Horizons") or click_element_by_text("Velvet Club"):
            time.sleep(5)
            print("On Details Screen - Capturing")
            adb("shell screencap -p /data/local/tmp/02_details.png")
            adb("pull /data/local/tmp/02_details.png screenshots/02_details.png")
            
            adb("shell input keyevent 4") # Back
            time.sleep(3)
    else:
        print("Failed to reach Home screen")
        # Capture whatever we have to debug
        adb("shell screencap -p /data/local/tmp/debug.png")
        adb("pull /data/local/tmp/debug.png screenshots/debug_failure.png")
        
    # 7. Search
    # "Requests" text might be the tab name if it matches standard Overseerr
    if click_element_by_text("Search") or click_element_by_text("Requests"):
        time.sleep(3)
        adb("shell input text 'Neon'")
        adb("shell input keyevent 66") # Enter
        time.sleep(5)
        print("On Search Screen - Capturing")
        adb("shell screencap -p /data/local/tmp/03_search.png")
        adb("pull /data/local/tmp/03_search.png screenshots/03_search.png")
    
    # 8. Profile
    if click_element_by_text("Settings") or click_element_by_text("Profile"):
        time.sleep(3)
        print("On Profile Screen - Capturing")
        adb("shell screencap -p /data/local/tmp/04_profile.png")
        adb("pull /data/local/tmp/04_profile.png screenshots/04_profile.png")

    print("Done")
    # adb("emu kill")

if __name__ == "__main__":
    main()
