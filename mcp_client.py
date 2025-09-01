#!/usr/bin/env python3
"""
Simple MCP client to connect to the Flight MCP Server
"""

import json
import requests
import sys
from typing import Dict, Any

class FlightMcpClient:
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

def main():
    client = FlightMcpClient()
    
    print("🛫 Flight MCP Client")
    print("=" * 50)
    
    # List available tools
    print("Available tools:")
    tools_response = client.list_tools()
    if "result" in tools_response and "tools" in tools_response["result"]:
        for tool in tools_response["result"]["tools"]:
            print(f"  - {tool['name']}: {tool['description']}")
    else:
        print(f"Error: {tools_response}")
        return
    
    print("\n" + "=" * 50)
    
    # Interactive mode
    while True:
        try:
            print("\nEnter flight search details (or 'quit' to exit):")
            departure = input("Departure city/airport: ").strip()
            if departure.lower() == 'quit':
                break
            
            arrival = input("Arrival city/airport: ").strip()
            if arrival.lower() == 'quit':
                break
                
            date = input("Date (YYYY-MM-DD): ").strip()
            if date.lower() == 'quit':
                break
            
            print(f"\n🔍 Searching flights from {departure} to {arrival} on {date}...")
            
            result = client.search_flights(departure, arrival, date)
            
            if "result" in result and "content" in result["result"]:
                print("\n✈️ Flight Results:")
                print("-" * 50)
                for content in result["result"]["content"]:
                    if content.get("type") == "text":
                        print(content["text"])
            elif "result" in result:
                print(f"\n✈️ Flight Results:")
                print("-" * 50)
                print(result["result"])
            else:
                print(f"\n❌ Error: {result}")
                
        except KeyboardInterrupt:
            print("\n\nGoodbye! ✈️")
            break
        except Exception as e:
            print(f"\n❌ Error: {e}")

if __name__ == "__main__":
    main()