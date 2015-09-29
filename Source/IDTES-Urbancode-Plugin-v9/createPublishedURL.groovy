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
def publishSetName = props['publishSetName']
def urlPermissions = props['urlPermissions']
def urlPassword = props['urlPassword']
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Create Published URL Info:"
println "	Environment ID: " + configID
println "	URL Permissions: " + urlPermissions
println "	URL Password: " + urlPassword
println "Done"

def IDTESRESTClient = new RESTClient('https://cloud.skytap.com/')
IDTESRESTClient.defaultRequestHeaders.'Authorization: Basic' = encodedAuthString
IDTESRESTClient.defaultRequestHeaders.'Accept' = "application/json"
IDTESRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"

locked = 1
while (locked == 1) {
	locked = 0
	try {
		response = IDTESRESTClient.get(path: "configurations/" + configID)
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
}

vmList = response.data.vms

publishSetBody = "{\"publish_set\":{\"publish_set_type\":\"single_url\",\"vms\":["
vmCounter = 0
vmList.each {
	if (vmCounter == 0) {
		publishSetBody = publishSetBody + "{\"access\":\"" + urlPermissions + "\",\"vm_ref\":\"" + it.id + "\"}"
		vmCounter = vmCounter + 1
        } else {
		publishSetBody = publishSetBody + ",{\"access\":\"" + urlPermissions + "\",\"vm_ref\":\"" + it.id + "\"}"
		vmCounter = vmCounter + 1
        }
}

if (urlPassword) {
	passwordString = "\"" + urlPassword + "\""
} else {
	passwordString = "null"
}
publishSetBody = publishSetBody + "],\"password\":" + passwordString + ",\"name\":\"" + publishSetName + "\"}}"

locked = 1
while (locked == 1) {
	locked = 0
	try {
		response = IDTESRESTClient.post(path: "configurations/" + configID + "/publish_sets",
			requestContentType: ContentType.TEXT,
			body: publishSetBody)
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
}

desktopsURL = response.data.desktops_url

println "Setting publishedURL property to: " + desktopsURL
apTool.setOutputProperty("publishedURL", desktopsURL)
apTool.setOutputProperties()
