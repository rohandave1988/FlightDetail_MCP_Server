#!/usr/bin/env python3
"""
Comprehensive MCP client to connect to the Travel & Content Search MCP Server
Supports Flight, TripAdvisor, and YouTube search functionality
"""

import json
import requests
import sys
from typing import Dict, Any, Optional

class TravelMcpClient:
    def __init__(self, server_url: str = "http://localhost:8080/mcp"):
        self.server_url = server_url
        self.session = requests.Session()
    
    def send_request(self, method: str, params: Dict[str, Any] = None, request_id: int = 1) -> Dict[str, Any]:
        """Send a JSON-RPC 2.0 request to the MCP server"""
        payload = {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": method
        }
        if params:
            payload["params"] = params
        
        try:
            response = self.session.post(
                self.server_url,
                json=payload,
                headers={"Content-Type": "application/json"}
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            return {"error": f"Request failed: {e}"}
    
    def list_tools(self) -> Dict[str, Any]:
        """List available tools"""
        return self.send_request("tools/list", request_id=1)
    
    def search_flights(self, departure: str, arrival: str, date: str) -> Dict[str, Any]:
        """Search for flights"""
        return self.send_request("tools/call", {
            "name": "search_flights",
            "arguments": {
                "departure": departure,
                "arrival": arrival,
                "date": date
            }
        }, request_id=2)
    
    def search_hotels(self, location: str, check_in: str, check_out: str, adults: int = 2) -> Dict[str, Any]:
        """Search for hotels"""
        return self.send_request("tools/call", {
            "name": "search_hotels",
            "arguments": {
                "location": location,
                "checkIn": check_in,
                "checkOut": check_out,
                "adults": adults
            }
        }, request_id=3)
    
    def search_restaurants(self, location: str, cuisine: Optional[str] = None) -> Dict[str, Any]:
        """Search for restaurants"""
        arguments = {"location": location}
        if cuisine:
            arguments["cuisine"] = cuisine
        
        return self.send_request("tools/call", {
            "name": "search_restaurants",
            "arguments": arguments
        }, request_id=4)
    
    def search_attractions(self, location: str, category: Optional[str] = None) -> Dict[str, Any]:
        """Search for attractions"""
        arguments = {"location": location}
        if category:
            arguments["category"] = category
        
        return self.send_request("tools/call", {
            "name": "search_attractions",
            "arguments": arguments
        }, request_id=5)
    
    def search_youtube_videos(self, query: str, duration: Optional[str] = None, 
                             upload_date: Optional[str] = None, sort_by: Optional[str] = None,
                             max_results: int = 20) -> Dict[str, Any]:
        """Search for YouTube videos"""
        arguments = {"query": query, "maxResults": max_results}
        if duration:
            arguments["duration"] = duration
        if upload_date:
            arguments["uploadDate"] = upload_date
        if sort_by:
            arguments["sortBy"] = sort_by
        
        return self.send_request("tools/call", {
            "name": "search_youtube_videos",
            "arguments": arguments
        }, request_id=6)

def display_result(result: Dict[str, Any], service_emoji: str, service_name: str):
    """Display search results in a consistent format"""
    if "result" in result and "content" in result["result"]:
        print(f"\n{service_emoji} {service_name} Results:")
        print("-" * 60)
        for content in result["result"]["content"]:
            if content.get("type") == "text":
                # Try to format JSON nicely
                try:
                    data = json.loads(content["text"])
                    print(json.dumps(data, indent=2, ensure_ascii=False))
                except json.JSONDecodeError:
                    print(content["text"])
    elif "result" in result:
        print(f"\n{service_emoji} {service_name} Results:")
        print("-" * 60)
        print(result["result"])
    else:
        print(f"\n❌ Error: {result}")

def handle_flight_search(client: TravelMcpClient):
    """Handle flight search interaction"""
    print("\n✈️ Flight Search")
    print("=" * 30)
    
    departure = input("Departure city/airport: ").strip()
    if not departure:
        return
        
    arrival = input("Arrival city/airport: ").strip()
    if not arrival:
        return
        
    date = input("Date (YYYY-MM-DD): ").strip()
    if not date:
        return
    
    print(f"\n🔍 Searching flights from {departure} to {arrival} on {date}...")
    result = client.search_flights(departure, arrival, date)
    display_result(result, "✈️", "Flight")

def handle_hotel_search(client: TravelMcpClient):
    """Handle hotel search interaction"""
    print("\n🏨 Hotel Search")
    print("=" * 30)
    
    location = input("Location: ").strip()
    if not location:
        return
        
    check_in = input("Check-in date (YYYY-MM-DD): ").strip()
    if not check_in:
        return
        
    check_out = input("Check-out date (YYYY-MM-DD): ").strip()
    if not check_out:
        return
    
    adults_input = input("Number of adults (default: 2): ").strip()
    adults = int(adults_input) if adults_input.isdigit() else 2
    
    print(f"\n🔍 Searching hotels in {location} for {adults} adults...")
    result = client.search_hotels(location, check_in, check_out, adults)
    display_result(result, "🏨", "Hotel")

def handle_restaurant_search(client: TravelMcpClient):
    """Handle restaurant search interaction"""
    print("\n🍽️ Restaurant Search")
    print("=" * 30)
    
    location = input("Location: ").strip()
    if not location:
        return
        
    cuisine = input("Cuisine (optional): ").strip()
    cuisine = cuisine if cuisine else None
    
    print(f"\n🔍 Searching restaurants in {location}...")
    result = client.search_restaurants(location, cuisine)
    display_result(result, "🍽️", "Restaurant")

def handle_attraction_search(client: TravelMcpClient):
    """Handle attraction search interaction"""
    print("\n🎭 Attraction Search")
    print("=" * 30)
    
    location = input("Location: ").strip()
    if not location:
        return
        
    category = input("Category (optional, e.g., museums, parks, tours): ").strip()
    category = category if category else None
    
    print(f"\n🔍 Searching attractions in {location}...")
    result = client.search_attractions(location, category)
    display_result(result, "🎭", "Attraction")

def handle_youtube_search(client: TravelMcpClient):
    """Handle YouTube video search interaction"""
    print("\n📺 YouTube Video Search")
    print("=" * 30)
    
    query = input("Search query: ").strip()
    if not query:
        return
    
    print("\nOptional filters:")
    duration = input("Duration (short/medium/long): ").strip()
    duration = duration if duration in ["short", "medium", "long"] else None
    
    upload_date = input("Upload date (hour/today/week/month/year): ").strip()
    upload_date = upload_date if upload_date in ["hour", "today", "week", "month", "year"] else None
    
    sort_by = input("Sort by (relevance/upload_date/view_count/rating): ").strip()
    sort_by = sort_by if sort_by in ["relevance", "upload_date", "view_count", "rating"] else None
    
    max_results_input = input("Max results (1-50, default: 20): ").strip()
    max_results = int(max_results_input) if max_results_input.isdigit() and 1 <= int(max_results_input) <= 50 else 20
    
    print(f"\n🔍 Searching YouTube videos for '{query}'...")
    result = client.search_youtube_videos(query, duration, upload_date, sort_by, max_results)
    display_result(result, "📺", "YouTube")

def display_menu():
    """Display the main menu"""
    print("\n" + "="*60)
    print("🌍 Travel & Content Search MCP Client")
    print("="*60)
    print("Choose a service:")
    print("1. ✈️  Flight Search")
    print("2. 🏨  Hotel Search")
    print("3. 🍽️  Restaurant Search") 
    print("4. 🎭  Attraction Search")
    print("5. 📺  YouTube Video Search")
    print("6. 🔧  List Available Tools")
    print("7. 🚪  Exit")
    print("="*60)

def main():
    client = TravelMcpClient()
    
    print("🌍 Travel & Content Search MCP Client")
    print("Connecting to server...")
    
    # Test connection by listing tools
    tools_response = client.list_tools()
    if "error" in tools_response:
        print(f"❌ Failed to connect to server: {tools_response['error']}")
        print("Make sure the MCP server is running on http://localhost:8080")
        return
    
    print("✅ Connected successfully!")
    
    while True:
        try:
            display_menu()
            choice = input("\nEnter your choice (1-7): ").strip()
            
            if choice == "1":
                handle_flight_search(client)
            elif choice == "2":
                handle_hotel_search(client)
            elif choice == "3":
                handle_restaurant_search(client)
            elif choice == "4":
                handle_attraction_search(client)
            elif choice == "5":
                handle_youtube_search(client)
            elif choice == "6":
                print("\n🔧 Available Tools:")
                print("-" * 50)
                if "result" in tools_response and "tools" in tools_response["result"]:
                    for i, tool in enumerate(tools_response["result"]["tools"], 1):
                        print(f"{i}. {tool['name']}")
                        print(f"   Description: {tool['description']}")
                        print()
                else:
                    print(f"❌ Error listing tools: {tools_response}")
            elif choice == "7":
                print("\n👋 Goodbye! Thanks for using Travel & Content Search MCP Client!")
                break
            else:
                print("\n❌ Invalid choice. Please enter 1-7.")
                
        except KeyboardInterrupt:
            print("\n\n👋 Goodbye! Thanks for using Travel & Content Search MCP Client!")
            break
        except Exception as e:
            print(f"\n❌ Error: {e}")
            print("Please try again.")

# Also keep backward compatibility
FlightMcpClient = TravelMcpClient

if __name__ == "__main__":
    main()