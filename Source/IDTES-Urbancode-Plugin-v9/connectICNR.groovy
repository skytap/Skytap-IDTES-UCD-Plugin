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

def apTool = new AirPluginTool(this.args[0], this.args[1])
props = apTool.getStepProperties()
def sourceConfigID = props['sourceConfigID']
def sourceNetName = props['sourceNetName']
def targetConfigID = props['targetConfigID']
def targetNetName = props['targetNetName']
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Connect Environments using ICNR Command Info:"
println "	Source Environment ID: " + sourceConfigID
println "	Source Network Name: " + sourceNetName
println "	Destination Environment ID: " + targetConfigID
println "	Destination Network Name: " + targetNetName
println "	User Name: " + username
println "	Password: " + password
println "Done"

def IDTESRESTClient = new RESTClient('https://cloud.skytap.com/')
IDTESRESTClient.defaultRequestHeaders.'Authorization: Basic' = encodedAuthString
IDTESRESTClient.defaultRequestHeaders.'Accept' = "application/json"
IDTESRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"

busy = 0
response = IDTESRESTClient.get(path: "configurations/" + sourceConfigID)
println "Initial Source Environment State is \"" + response.data.runstate + "\""
sourceNetID = 0
sourceNetList = response.data.networks

sourceNetList.each {
        if (it.name == sourceNetName) {
                println "Found Source Network Name: " + it.name
                println "Source Network ID: " + it.id
                sourceNetID = it.id
        }
}
if (sourceNetID == 0) {
	System.err.println "Error: Source Network \"" + sourceNetName + "\" not found."
	System.exit(1)
}

if (response.data.runstate == "busy") {
	busy = 1
}
response = IDTESRESTClient.get(path: "configurations/" + targetConfigID)
println "Initial Destination Environment State is \"" + response.data.runstate + "\""
destNetID = 0
destNetList = response.data.networks

destNetList.each {
        if (it.name == targetNetName) {
                println "Found Destination Network Name: " + it.name
                println "Destination Network ID: " + it.id
                destNetID = it.id
        }
}
if (destNetID == 0) {
	System.err.println "Error: Destination Network \"" + targetNetName + "\" not found."
	System.exit(1)
}
if (response.data.runstate == "busy") {
	busy = 1
}
def loopCounter = 1
while ((busy == 1) && (loopCounter <= 12)) {
	busy = 0
	println "At least one environment is busy, waiting for it to be ready..."
	sleep(10000)
	loopCounter = loopCounter + 1
	response = IDTESRESTClient.get(path: "configurations/" + sourceConfigID)
	println "Source Environment State is \"" + response.data.runstate + "\""
	if (response.data.runstate == "busy") {
		busy = 1
	}
	response = IDTESRESTClient.get(path: "configurations/" + targetConfigID)
	println "Destination Environment State is \"" + response.data.runstate + "\""
	if (response.data.runstate == "busy") {
		busy = 1
	}
}

if (busy == 0) {
	def locked = 0
	println "Connecting Networks via ICNR"
	try {
		response = IDTESRESTClient.post(path: "tunnels/", query:[source_network_id:sourceNetID, target_network_id:destNetID])
	} catch (HttpResponseException ex) {
		if (ex.statusCode == 423) {
			println "Resource is locked.  Retrying..."
			locked = 1
			sleep(5000)
		} else if (ex.statusCode == 404) {
			println "Resource is temporarily unavailable.  Retrying..."
			locked = 1
			sleep(5000)
		} else {
			println "Unexpected Error: " + ex.statusCode
			System.exit(1)
		}
	}
	while (locked == 1) {
		try {
			locked = 0
			response = IDTESRESTClient.post(path: "tunnels/", query:[source_network_id:sourceNetID, target_network_id:destNetID])
		} catch (HttpResponseException ex) {
			if (ex.statusCode == 423) {
				println "Resource is locked.  Retrying..."
				locked = 1
				sleep(5000)
			} else if (ex.statusCode == 404) {
				println "Resource is temporarily unavailable.  Retrying..."
				locked = 1
				sleep(5000)
			} else {
				println "Unexpected Error. REST return status: " + ex.statusCode
				System.exit(1)
			}
		}
	}
} else {
	System.err.println "Error: Environment " + targetConfigID + " never left the \"busy\" state"
	System.exit(1)
}
