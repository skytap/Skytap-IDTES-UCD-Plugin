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
def templateName = props['templateName']
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Create Template from Environment Command Info:"
println "	Environment ID: " + configID
println "	Template Name: " + templateName
println "Done"

def IDTESRESTClient = new RESTClient('https://cloud.skytap.com/')
IDTESRESTClient.defaultRequestHeaders.'Authorization: Basic' = encodedAuthString
IDTESRESTClient.defaultRequestHeaders.'Accept' = "application/json"
IDTESRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"


def locked = 0
try {
response = IDTESRESTClient.post(path: "templates", 
	query:[configuration_id:configID],
	requestContentType: ContentType.JSON)
} catch (HttpResponseException ex) {
	if (ex.statusCode == 423) {
		println "Environment " + configID + " locked. Retrying..."
		locked = 1
		sleep(5000)
	} else {
		System.err.println "Unexpected Error: " + ex.statusCode + " - " + ex.getMessage()
		System.exit(1)
	}
}

while (locked == 1) {
	try {
		locked = 0
		response = IDTESRESTClient.post(path: "configurations",
			body: ['template_id':templateID],
			requestContentType: ContentType.JSON)
	} catch (HttpResponseException ex) {
		if (ex.statusCode == 423) {
			println "Template " + templateID + " locked. Retrying..."
			locked = 1
			sleep(5000)
		} else {
			System.err.println "Unexpected Error: " + ex.statusCode + " - " + ex.getMessage()
			System.exit(1)
		}
	}
}

templateID = response.data.id
println "Template ID: " + templateID

if (templateName) {
	loopCounter = 1
	success = 0

	while ((success == 0) && (loopCounter <=10)) {
		println "Setting Template Name to \"" + templateName + "\""
		loopCounter = loopCounter + 1
		try {
			response = IDTESRESTClient.put(path: "templates/" + templateID, query:[name:templateName])
			success = 1
		} catch (HttpResponseException ex) {
			if (ex.statusCode == 400) {
				println "Message: " + ex.getMessage()
				println "Template " + templateID + " is temporarily unavailable. Retrying..."
				success = 0
				sleep(10000)
			} else {
				System.err.println "Unexpected Error: " + ex.statusCode + " - " + ex.getMessage()
				System.exit(1)
			}
		}
	}
	if ((success == 0) && (loopCounter > 10)) {
		System.err.println "Failed to set Template Name. Exiting."
		System.exit(1)
	}
}

apTool.setOutputProperty("templateID", templateID)
apTool.setOutputProperties()
