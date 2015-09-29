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

def configID = props['configID']
def templateID = props['templateID']
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Add Template to Environment Info:"
println "	Environment ID: " + configID
println "	Template ID: " + templateID
println "Done"

def IDTESRESTClient = new RESTClient('https://cloud.skytap.com/')
IDTESRESTClient.defaultRequestHeaders.'Authorization: Basic' = encodedAuthString
IDTESRESTClient.defaultRequestHeaders.'Accept' = "application/json"
IDTESRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"

loopCounter = 1
locked = 1
while ((loopCounter <= 12) && (locked == 1)) {
	try {
		loopCounter = loopCounter + 1
		locked = 0
		response = IDTESRESTClient.put(path: "configurations/" + configID,
			body: ['template_id':templateID],
			requestContentType: ContentType.JSON)
	} catch(HttpResponseException ex) {
		if ((ex.statusCode == 423) || (ex.statusCode == 500)) {
			println "Environment is locked or busy. Retrying..."
			locked = 1
			sleep(10000)
		} else {
			System.err.println "Unexpected Error: " + ex.statusCode + " - " + ex.getMessage()
			System.exit(1)
		}
	}
}

println "Added Template " + templateID + " to Environment " + configID


