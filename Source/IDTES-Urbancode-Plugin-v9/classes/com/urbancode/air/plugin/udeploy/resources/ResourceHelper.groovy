/*
* Licensed Materials - Property of IBM Corp.
* IBM UrbanCode Deploy
* (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
*
* U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
* GSA ADP Schedule Contract with IBM Corp.
*/
package com.urbancode.air.plugin.udeploy.resources;
import com.urbancode.air.AirPluginTool
import com.urbancode.ud.client.AgentClient
import com.urbancode.ud.client.ResourceClient
import java.util.UUID
import java.util.List
import java.util.Map
import java.util.HashMap
import org.codehaus.jettison.json.JSONObject
public class ResourceHelper {
    def apTool
    def props = []
    def udUser
    def udPass
    def weburl
    ResourceClient client
    AgentClient agentClient
    
    public ResourceHelper(def apToolIn) {
        apTool = apToolIn
        props = apTool.getStepProperties()
        udUser = apTool.getAuthTokenUsername()
        udPass = apTool.getAuthToken()
        weburl = System.getenv("AH_WEB_URL")
        client = new ResourceClient(new URI(weburl), udUser, udPass)
        agentClient = new AgentClient(new URI(weburl), udUser, udPass)
        com.urbancode.air.XTrustProvider.install()
    }
    
    def checkIfResourceExists() {
        def resourceName = props['resource']
        
        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        
        try {
            JSONObject resourceJSON = client.getResourceByPath(resourceName);
            if (resourceJSON == null) {
                throw new IOException("no resource found")
            }
            println "Resource with name ${resourceName} was found."
            apTool.setOutputProperty("exists", "true");
        }
        catch(IOException e) {
            if(e.getMessage().contains("404") || e.getMessage().contains("no resource found")) {
                println "Request was successful but no resource with name ${resourceName} was found."
                apTool.setOutputProperty("exists", "false")
            }
            else {
                println "An error occurred during your request."
                throw new IOException(e);
            }
        }
        apTool.setOutputProperties();
    }
    
    def waitForResources() {
        def resourceNames = props['resources'].split("\n");
        def timeoutString = props['timeout'];

        def timeout = 0;
        if (timeoutString.trim().size() > 0) {
            try {
                timeout = Long.valueOf(timeoutString);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Timeout value is not a number: "+timeoutString);
            }
            if (timeout < 0) {
                throw new IllegalArgumentException("Timeout ("+timeout+") cannot be a negative number.");
            }
        }
        
        waitForResources(resourceNames, timeout)
    }
    
    def waitForResources(def resourceNames, def timeout) {
        def timeoutMs = timeout*1000;
        def startTime = new Date().getTime();
        def successful = false;
        while (new Date().getTime()-startTime < timeoutMs || timeout == 0) {
            def allPassed = true;
            
            println new Date().toString()+":";
            for (String resourceName : resourceNames) {
                JSONObject resourceJSON = client.getResourceByPath(resourceName);
                if (resourceJSON != null) {
                    def status = "OFFLINE";
                    
                    if (resourceJSON.has("status")) {
                        status = resourceJSON.getString("status");
                    }

                    if (!status.equals("ONLINE")) {
                        allPassed = false;
                    }

                    
                    println "    "+resourceJSON.getString("name")+": "+status
                }
                else {
                    allPassed = false;
                    println "    "+resourceName+" not found"
                }
            }
            println ""
            
            if (allPassed) {
                successful = true;
                break;
            }
            
            sleep(10000);
        }
        
        if (successful) {
            println "All resources are online."
        }
        else {
            throw new IllegalArgumentException("Not all resources attained online status before timeout.");
        }
    }
    
    def waitForAgents(def agentNames, def timeout) {
        def timeoutMs = timeout*1000;
        def startTime = new Date().getTime();
        def successful = false;
        while (new Date().getTime()-startTime < timeoutMs || timeout == 0) {
            def allPassed = true;
            
            println new Date().toString()+":";
            for (String agentName : agentNames) {
                JSONObject agentJSON = null;
                try {
                    agentJSON = agentClient.getAgent(agentName);
                } catch (Exception e) {
                    //swallow -- Agent may not exist yet
                }
                if (agentJSON != null) {
                    def status = agentJSON.getString("status");
                
                    if (!status.equals("ONLINE")) {
                        allPassed = false;
                    }
                    
                    println "    "+agentJSON.getString("name")+": "+status
                }
                else {
                    allPassed = false;
                    println "    "+agentName+" not found"
                }
            }
            println ""
            
            if (allPassed) {
                successful = true;
                break;
            }
            
            sleep(10000);
        }
        
        if (successful) {
            println "All  agents are online."
        }
        else {
            throw new IllegalArgumentException("Not all agents attained online status before timeout.");
        }
    }
    
    def installAgent() {
        def name = props['name']
        def host = props['host']
        def port = props['port']
        def sshUsername = props['sshUsername']
        def sshPassword = props['sshPassword']
        def installDir = props['installDir']
        def javaHomePath = props['javaHomePath']
        def tempDirPath = props['tempDirPath']
        def serverHost = props['serverHost']
        def serverPort = props['serverPort']
        def proxyHost = props['proxyHost']
        def proxyPort = props['proxyPort']
        def mutualAuth = props['mutualAuth']
        
        agentClient.installAgent(name, host, port, sshUsername, sshPassword, installDir, javaHomePath,
                tempDirPath, serverHost, serverPort, proxyHost, proxyPort, mutualAuth)
    }
    
    def createResource() {
        def resourceName = props['name']
        def agentName = props['agent']
        def agentPoolName = props['agentPool']
        def parentName = props['parent']
        def roleName = props['role']
        
        def newResource = client.createResource(resourceName, agentName, agentPoolName, parentName, roleName)
        JSONObject resourceJson = new JSONObject(newResource);
        apTool.setOutputProperty("new.resource.name", resourceName)
        apTool.setOutputProperty("new.resource.id", resourceJson.get("id"))
        apTool.setOutputProperties()
    }
    
    def setAgentProperty() {
        def agentName = props['agent']
        def propName = props['name']
        def propValue = props['value']
        def isSecure = Boolean.valueOf(props['isSecure'])
        
        if (!propName) {
            throw new IllegalArgumentException("no property name was specified")
        }
        if (!propValue) {
            propValue = ""
        }
        if (!agentName) {
            throw new IllegalArgumentException("no agent was specified")
        }
        agentClient.setAgentProperty(agentName, propName, propValue, isSecure)
    }
    
    def setResourceProperty() {
        def resourceName = props['resource']
        def propName = props['name']
        def propValue = props['value']
        def isSecure = Boolean.valueOf(props['isSecure'])
        
        if (!propName) {
            throw new IllegalArgumentException("no property name was specified")
        }
        if (!propValue) {
            propValue = ""
        }
        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        client.setResourceProperty(resourceName, propName, propValue, isSecure)
    }
    
    def setResourceRoleProperty() {
        def resourceName = props['resource']
        def roleName = props['role']
        def propName = props['name']
        def propValue = props['value']
        
        if (!propName) {
            throw new IllegalArgumentException("no property name was specified")
        }
        if (!propValue) {
            propValue = ""
        }
        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        if (!roleName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        client.setResourceRoleProperty(roleName, resourceName, propName, propValue)
    }
    
    def checkIfResourceHasRole() {
        def resourcePath = props['resource']
        def roleName = props['role']
        
        if (!resourcePath) {
            throw new IllegalArgumentException("no resource was specified")
        }
        if (!roleName) {
            throw new IllegalArgumentException("no role was specified")
        }
        def resource = client.getResourceByPath(resourcePath);
        if (!resource.role.name.equals(roleName)) {
            throw new RuntimeException("The request went through, but the role was not on the resource.")
        }
        
    }
    
    def addRoleToResource() {
        def resourceName = props['resource']
        def roleName = props['role']
        
        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        if (!roleName) {
            throw new IllegalArgumentException("no role was specified")
        }
        Map<String, String> emptyMap = new HashMap<String, String>()
        client.addRoleToResource(resourceName, roleName, emptyMap)
    }
    
    def removeRoleFromResource() {
        def resourceName = props['resource']
        def roleName = props['role']
        
        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        if (!roleName) {
            throw new IllegalArgumentException("no role was specified")
        }
        client.removeRoleFromResource(resourceName, roleName)
    }
    
    def deleteResource() {
        def resourceName = props['resource']
        
        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        client.deleteResource(resourceName)
        println "deleted resource " + resourceName
    }
    
    def deleteAgent() {
        def agentName = props['agent']
        
        if (!agentName) {
            throw new IllegalArgumentException("no agent was specified")
        }
        agentClient.deleteAgent(agentName)
        println "deleted agent " + agentName
    }
    
    def deleteManyResources() {
        String resourceNames = props['resources']
        
        if (!resourceNames) {
            throw new IllegalArgumentException("no resources were specified")
        }
        
        String[] resourceArray = resourceNames.split(",");
        
        for (String resource : resourceArray) {
            client.deleteResource(resource)
            println "deleted resource " + resource
        }
    }
    
    def deleteManyAgents() {
        String agentNames = props['agents']
        
        if (!agentNames) {
            throw new IllegalArgumentException("no agents were specified")
        }
        
        String[] agentArray = agentNames.split(",");
        
        for (String agent : agentArray) {
            agentClient.deleteAgent(agent)
            println "deleted agent " + agent
        }
    }
    
    def getAgentProperty() {
        String agentName = props['agent']
        String propName = props['name']
        
        if (!agentName) {
            throw new IllegalArgumentException("no agent was specified")
        }
        if (!propName) {
            throw new IllegalArgumentException("no property name was specified")
        }
        
        String propValue = agentClient.getAgentProperty(agentName, propName)
        
        println "property " + propName + " resolved to " + propValue
        apTool.setOutputProperty(propName, propValue)
        apTool.setOutputProperties()
    }
    
    def getResourceProperty() {
        String resourceName = props['resource']
        String propName = props['name']
        
        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        if (!propName) {
            throw new IllegalArgumentException("no property name was specified")
        }
        
        String propValue = client.getResourceProperty(resourceName, propName)
        
        println "property " + propName + " resolved to " + propValue
        apTool.setOutputProperty(propName, propValue)
        apTool.setOutputProperties()
    }
    
    def getResourceRoleProperty() {
        def resourceName = props['resource']
        def roleName = props['role']
        def propName = props['name']
        
        if (!propName) {
            throw new IllegalArgumentException("no property name was specified")
        }
        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        if (!roleName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        String propValue = client.getResourceRolePropertyForResource(roleName, resourceName, propName)
        println "property " + propName + " resolved to " + propValue
        apTool.setOutputProperty(propName, propValue)
        apTool.setOutputProperties()
    }
    
    def deleteResourceInventoryForComponent() {
        def resourceName = props['resource']
        def versionName = props['version']
        def statusName = props['status']
        def componentNames =
                props['component'].split("\n").findAll{it.trim().length() > 0}.collect{it.trim()}


        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        if (!componentNames) {
            throw new IllegalArgumentException("no components were specified")
        }
        if (!versionName || versionName == "*") {
            versionName = ""
        }
        if (!statusName || statusName == "*") {
            statusName = ""
        }

        
        println "Will remove inventory for the following components: "+componentNames.join(", ")

        componentNames.each{ componentName ->
            try {
                client.deleteResourceInventoryForComponent(
                        resourceName,
                        componentName,
                        versionName,
                        statusName)
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to delete inventory for component, '"+componentName+"'", e)
            }
        }
        println "Deleted all inventory entries that matched the input criteria."
    }
    
    public def getLatestVersionByResourceAndComponent() {
        String componentId = props['component']
        String resourceId = props['resource']
        String propName = props['name']
        
        if (!componentId) {
            throw new IllegalArgumentException("no component was specified")
        }
        if (!resourceId) {
            throw new IllegalArgumentException("no resource was specified")
        }
        
        JSONObject reply = client.getLatestVersionByResourceAndComponent(resourceId, componentId)
        
        if (reply.length() == 0) {
            println "No version for that component exists for that resource"
        }
        else {
            def verName = reply.get("version").getAt("name")
            def verId = reply.get("version").getAt("id")
            apTool.setOutputProperty("version.name", verName)
            apTool.setOutputProperty("version.id", verId)
            apTool.setOutputProperties()
            
            println "Version name '"+verName+"' and id '"+verId+"' have been added to the output properties"
        }
    }
    
    public def addResourceToTeam() {
        def resourceName = props['resource']
        def teamName = props['team']
        def typeName = props['type']
        
        if (!resourceName) {
            throw new IllegalArgumentException("no resource was specified")
        }
        if (!teamName) {
            throw new IllegalArgumentException("no team was specified")
        }

        client.addResourceToTeam(resourceName, teamName, typeName)
        println "Resource was added to team for the given type classification."
    }

    public def getAgentInfo() {
        def agentName = props['agent']
        if (!agentName) {
            throw new IllegalArgumentException("no agent was specified")
        }
    
        def agentJson = agentClient.getAgent(agentName)
        if (agentJson.has("status")) {
            agentJson.put("agentStatus", agentJson.get("status"))
            agentJson.put("status", null)
        }
        def agentInfoMap = agentClient.getJSONAsProperties(agentJson)
        for (String key : agentInfoMap.keySet()) {
            apTool.setOutputProperty(key, agentInfoMap.get(key))
        }
        apTool.setOutputProperties()
        
        println agentJson
    }
}

