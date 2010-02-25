<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     version="1.0">

  <xsl:template match="RealTime">
     <xsl:copy/>
  </xsl:template>

  <xsl:template match="Cluster">                     
     <xsl:copy-of select="self::node()"/>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>

