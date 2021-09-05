
<#if executionData.job.group??>
    <#assign jobName="${executionData.job.group} / ${executionData.job.name}">
<#else>
    <#assign jobName="${executionData.job.name}">
</#if>
<#assign message="Execution #${executionData.id}> of job|${jobName}">
<#if trigger == "start">
    <#assign state="Started">
<#elseif trigger == "failure">
    <#assign state="Failed">
<#elseif trigger == "avgduration">
    <#assign state="Average exceeded">
<#elseif trigger == "retryablefailure">
   <#assign state="Retry Failure">
<#else>
   <#assign state="Succeeded">
</#if>

{
  "content":"${message}",
  "embeds": [
    {
      "color": "${color}",
      "fields": [
        {
          "name":"Job Name",
          "value":"[${jobName}](${executionData.job.href})",
          "inline": true
        },
        {
          "name":"Project",
          "value":"${executionData.project}",
          "inline": true
        },
        {
          "name":"Status",
          "value":"${state}",
          "inline": true
        },
        {
          "name":"Execution ID",
          "value":"[#${executionData.id}](${executionData.href})",
          "inline": true
        },
        {
          "name":"Options",
          "value":"${(executionData.argstring?replace('"', '\''))!"N/A"}",
          "inline": true
        },
        {
          "name":"Started By",
          "value":"${executionData.user}",
          "inline": true
        }
        <#if trigger == "failure">
            ,{
               "name":"Failed Nodes",
               "value":"${executionData.failedNodeListString!"- (Job itself failed)"}",
               "inline":false
            }
</#if>
      ]
    }
  ]
}