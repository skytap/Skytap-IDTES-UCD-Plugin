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
def templateID = props['templateID']
def projectName = props['projectName']
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
def encodedAuthString = bytes.encodeBase64().toString()

println "Add Template to Project Info:"
println "	Template ID: " + templateID
println "	Project Name: " + projectName
println "Done"

def IDTESRESTClient = new RESTClient('https://cloud.skytap.com/')
IDTESRESTClient.defaultRequestHeaders.'Authorization: Basic' = encodedAuthString
IDTESRESTClient.defaultRequestHeaders.'Accept' = "application/json"
IDTESRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"

//
// Get the project ID of the specified Project Name
//

if (projectName) {
	projectID = 0
	response = IDTESRESTClient.get(path: "projects")
	projectList = response.data

	projectList.each {
        	if (it.name == projectName) {
                	println "Found Project Name: " + it.name
                	println "Project ID: " + it.id
                	projectID = it.id
        	}
	}
	if (projectID == 0) {
		System.err.println "Error: Project \"" + projectName + "\" not found."
		System.exit(1)
	}
}

try {
projadd_path = "projects/" + projectID + "/templates/" + templateID
response = IDTESRESTClient.post(path: projadd_path,
	requestContentType: ContentType.JSON)
} catch (HttpResponseException ex) {
	if (ex.statusCode == 423) {
		System.err.println "Template " + templateID + " locked."
		System.exit(1)
	} else {
		System.err.println "Unexpected Error: " + ex.statusCode
		System.exit(1)
	}
}

println "Added Template ID \"" + templateID + "\" to Project ID \"" + projectID + "\"" 
