<!-- MANY MANY thanks to Bob DuCharme for his article at 
     http://www.xml.com/pub/a/2002/10/02/tr.html [Accessed Jan 28/2010]
     and simple explanation of how to remove duplicate nodes via this
     xsl. 
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     version="1.0">

  <xsl:key name="WorkerNodeDups" match="WorkerNode/*" use="(@id)"/>
  <xsl:key name="HeadNodeDups" match="HeadNode/*" use="(@id)"/>


  <xsl:template match="WorkerNode/*">
      <xsl:if test="generate-id()=generate-id(key('WorkerNodeDups',@id)[1])">
      <xsl:copy>
         <xsl:apply-templates select="node()"/>
      </xsl:copy>
   </xsl:if>
  </xsl:template>
  <xsl:template match="HeadNode/*">
    <xsl:if test="generate-id()=generate-id(key('HeadNodeDups',@id)[1])">
     <xsl:copy>
        <xsl:apply-templates select="node()"/>
     </xsl:copy>
    </xsl:if>
  </xsl:template>
  <xsl:template match="PhysicalIP">
    <xsl:if test="not(. = preceding-sibling::PhysicalIP)">
      <xsl:copy>
        <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
    </xsl:if>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>

