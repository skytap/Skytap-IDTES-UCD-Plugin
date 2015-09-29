//
// Copyright 2015 Skytap Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException
import groovyx.net.http.ContentType
import com.urbancode.air.AirPluginTool
import com.urbancode.ud.client.ResourceClient
import org.codehaus.jettison.json.JSONObject

def apTool = new AirPluginTool(this.args[0], this.args[1])
props = apTool.getStepProperties()

def serverURL = props['serverURL']
def topLevelGroup = props['topLevelGroup']
def agentName = props['agentName']
def componentName = props['componentName']
def username = apTool.getAuthTokenUsername()
def password = apTool.getAuthToken()

weburl = System.getenv("AH_WEB_URL")
client = new ResourceClient(new URI(weburl), username, password)

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Bind Dynamic Agent to Component:"
println "	Server URL: " + serverURL
println "	Top Level UCD Group: " + topLevelGroup
println "	UCD Agent Name: " + agentName
println "	UCD Component Name: " + componentName
println "Done"

def IDTESRESTClient = new RESTClient(serverURL)
IDTESRESTClient.defaultRequestHeaders.'Authorization: Basic' = encodedAuthString
IDTESRESTClient.defaultRequestHeaders.'Accept' = "application/json"
IDTESRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"

//
// Register Agent with Top Level Group
//
parent = "/" + topLevelGroup
try {
def newResource = client.createResource("", agentName, "", parent, "")

} catch(HttpResponseException ex) {
 	println "Message: " + ex.getMessage()
 	println "Code: " + ex.statusCode
	System.exit(1)
}

        try {
            JSONObject resourceJSON = client.getResourceByPath(parent + "/" + agentName);
            if (resourceJSON == null) {
                throw new IOException("no resource found")
            }
            println "Agent registered as " + parent + "/" + agentName
        }
        catch(IOException e) {
            if(e.getMessage().contains("404") || e.getMessage().contains("no resource found")) {
                println "Request was successful but no resource with name ${resourceName} was found."
            }
            else {
                println "An error occurred during your request."
            }
            System.exit(1)
        }


//
// Associate Component with the Registered Agent
//
parent = "/" + topLevelGroup + "/" + agentName
try {
def newResource = client.createResource("", "", "", parent, componentName)

} catch(HttpResponseException ex) {
	println "Message: " + ex.getMessage()
	println "Code: " + ex.statusCode
	System.exit(1)
}

        try {
            JSONObject resourceJSON = client.getResourceByPath(parent + "/" + componentName);
            if (resourceJSON == null) {
                throw new IOException("no resource found")
            }
            println "Component associated with agent as " + parent + "/" + componentName
        }
        catch(IOException e) {
            if(e.getMessage().contains("404") || e.getMessage().contains("no resource found")) {
                println "Request was successful but no resource with name ${resourceName} was found."
            }
            else {
                println "An error occurred during your request."
            }
            System.exit(1)
        }


println "UCD Agent " + agentName + " has been added to the " + topLevelGroup + " UCD Top Level Group, and UCD Component " + componentName + " has been associated with the agent."


