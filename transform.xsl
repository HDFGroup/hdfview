<?xml version="1.0" ?>

<!-- This file strips hdfview.bat from heat.exe's generated allfiles.wxs
     so that it can manually be added and assigned an Id for associating
     file extensions to the application. -->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:wix="http://schemas.microsoft.com/wix/2006/wi">

  <!-- Copy all attributes and elements to the output. -->
  <xsl:template match="@*|*">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates select="*" />
    </xsl:copy>
  </xsl:template>

  <xsl:output method="xml" indent="yes" />

  <xsl:key name="bat-search" match="wix:Component[contains(wix:File/@Source, '.bat')]" use="@Id" />
  <xsl:template match="wix:Component[key('bat-search', @Id)]" />
  <xsl:template match="wix:ComponentRef[key('bat-search', @Id)]" />
</xsl:stylesheet>