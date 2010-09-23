<?xml version="1.0" encoding="UTF-8"?>
<!--
"""*
 * Copyright 2009 University of Victoria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * AUTHOR - Adam Bishop - ahbishop@uvic.ca
 * 
 * For comments or questions please contact the above e-mail address 
 * or Ian Gable - igable@uvic.ca
 *
 * """

-->


<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:key name="merge-siblings-on-node" match="Node/*" use="@node"/>

    <xsl:template match="Node//node()"/>

    <xsl:template match="WorkerNodes">
        <xsl:copy>
           <xsl:call-template name="nodeMerger">
               <xsl:with-param name="nodeList" select="current()/*"/>
           </xsl:call-template>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="nodeMerger">
       <xsl:param name="nodeList"/>

       <xsl:for-each select="$nodeList">
       
            <xsl:if test="generate-id(child::*) = generate-id(key('merge-siblings-on-node', */@node)[1])">
               <xsl:if test="key('merge-siblings-on-node',*/@node) != 0">
                 <Node>
                    <xsl:copy-of select="key('merge-siblings-on-node',*/@node)[node()]"/>
            
                 </Node>     
             </xsl:if>    
           </xsl:if>
       
       </xsl:for-each>
   </xsl:template>

  <xsl:template match="@*|node()">
     <xsl:copy>
      <xsl:apply-templates select="node()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
