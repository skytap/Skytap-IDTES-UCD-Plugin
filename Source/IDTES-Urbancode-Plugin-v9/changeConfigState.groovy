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

import com.urbancode.air.AirPluginTool
import groovyx.net.http.RESTClient
import groovyx.net.http.ContentType

def apTool = new AirPluginTool(this.args[0], this.args[1])
props = apTool.getStepProperties()
// def configID = apTool.getStepProperties()['configID']
def configID = props['configID']
def newState = props['newState']
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Change Environment State Command Info:"
println "	Environment ID: " + configID
println "	New State: " + newState
println "	User Name: " + username
println "	Password: " + password
println "Done"

def IDTESRESTClient = new RESTClient('https://cloud.skytap.com/')
IDTESRESTClient.defaultRequestHeaders.'Authorization: Basic' = encodedAuthString
IDTESRESTClient.defaultRequestHeaders.'Accept' = "application/json"
IDTESRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"


response = IDTESRESTClient.get(path: "configurations/" + configID)
println "Initial Run State is \"" + response.data.runstate + "\""

def loopCounter = 1
while ((response.data.runstate == "busy") && (loopCounter <= 12)) {
	println "Environment is busy, waiting for it to be ready..."
	sleep(10000)
	response = IDTESRESTClient.get(path: "configurations/" + configID)
	loopCounter = loopCounter + 1
}

if (response.data.runstate != "busy") {
	response = IDTESRESTClient.put(path: "configurations/" + configID, query:[runstate:newState] )

	def innerLoopCounter = 1
	while ((response.data.runstate != newState) && (innerLoopCounter <= 30)) {
		println "Waiting for environment " + configID + " to transition to \"" + newState + "\" state"
		sleep(10000)
		println "Checking on environment state"
		response = IDTESRESTClient.get(path: "configurations/" + configID)
		println "Run State is \"" + response.data.runstate + "\""
		if ((response.data.runstate != "busy") && (response.data.runstate != newState)) {
			response = IDTESRESTClient.put(path: "configurations/" + configID, query:[runstate:newState])
		}
		innerLoopCounter = innerLoopCounter + 1
	}
	if (response.data.runstate != newState) {
		System.err.println "Error: Environment " + configID + " never reached the " + newState + " state"
		System.exit(1)
	}
} else {
	System.err.println "Error: Environment " + configID + " never left the \"busy\" state"
	System.exit(1)
}
