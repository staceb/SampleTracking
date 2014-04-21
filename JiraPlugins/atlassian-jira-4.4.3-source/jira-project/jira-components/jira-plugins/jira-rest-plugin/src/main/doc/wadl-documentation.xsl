<?xml version="1.0" encoding="UTF-8"?>
<!--
  wadl_documentation.xsl (2008-12-09)

  An XSLT stylesheet for generating HTML documentation from WADL,
  by Mark Nottingham <mnot@yahoo-inc.com>.

  Copyright (c) 2006-2008 Yahoo! Inc.

  This work is licensed under the Creative Commons Attribution-ShareAlike 2.5
  License. To view a copy of this license, visit
    http://creativecommons.org/licenses/by-sa/2.5/
  or send a letter to
    Creative Commons
    543 Howard Street, 5th Floor
    San Francisco, California, 94105, USA
-->
<!--
 * FIXME
    - Doesn't inherit query/header params from resource/@type
    - XML schema import, include, redefine don't import
-->
<!--
  * TODO
    - forms
    - link to or include non-schema variable type defs (as a separate list?)
    - @href error handling
-->

<xsl:stylesheet
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
 xmlns:wadl="http://research.sun.com/wadl/2006/10"
 xmlns:xs="http://www.w3.org/2001/XMLSchema"
 xmlns:html="http://www.w3.org/1999/xhtml"
 xmlns:exsl="http://exslt.org/common"
 xmlns:ns="urn:namespace"
 extension-element-prefixes="exsl"
 xmlns="http://www.w3.org/1999/xhtml"
 exclude-result-prefixes="xsl wadl xs html ns"
>

    <xsl:output
        method="html"
        encoding="UTF-8"
        indent="yes"
        doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
        doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
    />

    <xsl:variable name="wadl-ns">http://research.sun.com/wadl/2006/10</xsl:variable>


    <xsl:template match="code">
        <code class="json"/>
    </xsl:template>

    <!-- expand @hrefs, @types into a full tree -->

    <xsl:variable name="resources">
        <xsl:apply-templates select="/wadl:application/wadl:resources" mode="expand"/>
    </xsl:variable>

    <xsl:template match="wadl:resources" mode="expand">
        <xsl:variable name="base">
            <xsl:choose>
                <xsl:when test="substring(@base, string-length(@base), 1) = '/'">
                    <xsl:value-of select="substring(@base, 1, string-length(@base) - 1)"/>
                </xsl:when>
                <xsl:otherwise><xsl:value-of select="@base"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:element name="resources" namespace="{$wadl-ns}">
            <xsl:for-each select="namespace::*">
                <xsl:variable name="prefix" select="name(.)"/>
                <xsl:if test="$prefix">
                    <xsl:attribute name="ns:{$prefix}"><xsl:value-of select="."/></xsl:attribute>
                </xsl:if>
            </xsl:for-each>
            <xsl:apply-templates select="@*|node()" mode="expand">
                <xsl:with-param name="base" select="$base"/>
            </xsl:apply-templates>
        </xsl:element>
    </xsl:template>

    <xsl:template match="wadl:resource[@type]" mode="expand" priority="1">
        <xsl:param name="base"></xsl:param>
        <xsl:variable name="uri" select="substring-before(@type, '#')"/>
        <xsl:variable name="id" select="substring-after(@type, '#')"/>
        <xsl:element name="resource" namespace="{$wadl-ns}">
			<xsl:attribute name="path"><xsl:value-of select="@path"/></xsl:attribute>
            <xsl:choose>
                <xsl:when test="$uri">
                    <xsl:variable name="included" select="document($uri, /)"/>
                    <xsl:copy-of select="$included/descendant::wadl:resource_type[@id=$id]/@*"/>
                    <xsl:attribute name="id"><xsl:value-of select="@type"/>#<xsl:value-of select="@path"/></xsl:attribute>
                    <xsl:apply-templates select="$included/descendant::wadl:resource_type[@id=$id]/*" mode="expand">
                        <xsl:with-param name="base" select="$uri"/>
                    </xsl:apply-templates>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="//resource_type[@id=$id]/@*"/>
                    <xsl:attribute name="id"><xsl:value-of select="$base"/>#<xsl:value-of select="@type"/>#<xsl:value-of select="@path"/></xsl:attribute>
                    <xsl:apply-templates select="//wadl:resource_type[@id=$id]/*" mode="expand">
                        <xsl:with-param name="base" select="$base"/>
                    </xsl:apply-templates>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="node()" mode="expand">
                <xsl:with-param name="base" select="$base"/>
            </xsl:apply-templates>
        </xsl:element>
    </xsl:template>

    <xsl:template match="wadl:*[@href]" mode="expand">
        <xsl:param name="base"></xsl:param>
        <xsl:variable name="uri" select="substring-before(@href, '#')"/>
        <xsl:variable name="id" select="substring-after(@href, '#')"/>
        <xsl:element name="{local-name()}" namespace="{$wadl-ns}">
            <xsl:copy-of select="@*"/>
            <xsl:choose>
                <xsl:when test="$uri">
                    <xsl:attribute name="id"><xsl:value-of select="@href"/></xsl:attribute>
                    <xsl:variable name="included" select="document($uri, /)"/>
                    <xsl:apply-templates select="$included/descendant::wadl:*[@id=$id]/*" mode="expand">
                        <xsl:with-param name="base" select="$uri"/>
                    </xsl:apply-templates>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="id"><xsl:value-of select="$base"/>#<xsl:value-of select="$id"/></xsl:attribute>
                    <!-- xsl:attribute name="id"><xsl:value-of select="generate-id()"/></xsl:attribute -->
                    <xsl:attribute name="element"><xsl:value-of select="//wadl:*[@id=$id]/@element"/></xsl:attribute>
                    <xsl:attribute name="mediaType"><xsl:value-of select="//wadl:*[@id=$id]/@mediaType"/></xsl:attribute>
                    <xsl:attribute name="status"><xsl:value-of select="//wadl:*[@id=$id]/@status"/></xsl:attribute>
                    <xsl:attribute name="name"><xsl:value-of select="//wadl:*[@id=$id]/@name"/></xsl:attribute>
                    <xsl:apply-templates select="//wadl:*[@id=$id]/*" mode="expand">
                        <xsl:with-param name="base" select="$base"/>
                    </xsl:apply-templates>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template match="node()[@id]" mode="expand">
        <xsl:param name="base"></xsl:param>
        <xsl:element name="{local-name()}" namespace="{$wadl-ns}">
            <xsl:copy-of select="@*"/>
            <xsl:attribute name="id"><xsl:value-of select="$base"/>#<xsl:value-of select="@id"/></xsl:attribute>
            <!-- xsl:attribute name="id"><xsl:value-of select="generate-id()"/></xsl:attribute -->
            <xsl:apply-templates select="node()" mode="expand">
                <xsl:with-param name="base" select="$base"/>
            </xsl:apply-templates>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@*|node()" mode="expand">
        <xsl:param name="base"></xsl:param>
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" mode="expand">
                <xsl:with-param name="base" select="$base"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

<!-- debug $resources
    <xsl:template match="/">
    <xsl:copy-of select="$resources"/>
    </xsl:template>
-->

    <!-- collect grammars (TODO: walk over $resources instead) -->

    <xsl:variable name="grammars">
        <xsl:copy-of select="/wadl:application/wadl:grammars/*[not(namespace-uri()=$wadl-ns)]"/>
        <xsl:apply-templates select="/wadl:application/wadl:grammars/wadl:include[@href]" mode="include-grammar"/>
        <xsl:apply-templates select="/wadl:application/wadl:resources/descendant::wadl:resource[@type]" mode="include-href"/>
        <xsl:apply-templates select="exsl:node-set($resources)/descendant::wadl:*[@href]" mode="include-href"/>
    </xsl:variable>

    <xsl:template match="wadl:include[@href]" mode="include-grammar">
        <xsl:variable name="included" select="document(@href, /)/*"></xsl:variable>
        <xsl:element name="wadl:include">
            <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
            <xsl:copy-of select="$included"/> <!-- FIXME: xml-schema includes, etc -->
        </xsl:element>
    </xsl:template>

    <xsl:template match="wadl:*[@href]" mode="include-href">
        <xsl:variable name="uri" select="substring-before(@href, '#')"/>
        <xsl:if test="$uri">
            <xsl:variable name="included" select="document($uri, /)"/>
            <xsl:copy-of select="$included/wadl:application/wadl:grammars/*[not(namespace-uri()=$wadl-ns)]"/>
            <xsl:apply-templates select="$included/descendant::wadl:include[@href]" mode="include-grammar"/>
            <xsl:apply-templates select="$included/wadl:application/wadl:resources/descendant::wadl:resource[@type]" mode="include-href"/>
            <xsl:apply-templates select="$included/wadl:application/wadl:resources/descendant::wadl:*[@href]" mode="include-href"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="wadl:resource[@type]" mode="include-href">
        <xsl:variable name="uri" select="substring-before(@type, '#')"/>
        <xsl:if test="$uri">
            <xsl:variable name="included" select="document($uri, /)"/>
            <xsl:copy-of select="$included/wadl:application/wadl:grammars/*[not(namespace-uri()=$wadl-ns)]"/>
            <xsl:apply-templates select="$included/descendant::wadl:include[@href]" mode="include-grammar"/>
            <xsl:apply-templates select="$included/wadl:application/wadl:resources/descendant::wadl:resource[@type]" mode="include-href"/>
            <xsl:apply-templates select="$included/wadl:application/wadl:resources/descendant::wadl:*[@href]" mode="include-href"/>
        </xsl:if>
    </xsl:template>

    <!-- main template -->

    <xsl:template match="/wadl:application">
        <html>
            <head>
                <title>
                    <xsl:choose>
                        <xsl:when test="wadl:doc[@title]">
                            <xsl:value-of select="wadl:doc[@title][1]/@title"/>
                        </xsl:when>
                        <xsl:otherwise>My Web Application</xsl:otherwise>
                    </xsl:choose>
                </title>
                <style type="text/css">
                    body {
                        font-family: sans-serif;
                        font-size: 0.85em;
                        margin: 2em 8em;
                    }
                    .methods {
                        background-color: #eef;
                        padding: 1em;
                    }
                    h1 {
                        font-size: 2.5em;
                    }
                    h2 {
                        border-bottom: 1px solid black;
                        margin-top: 1em;
                        margin-bottom: 0.5em;
                        font-size: 2em;
                       }
                    h3 {
                        color: orange;
                        font-size: 1.75em;
                        margin-top: 1.25em;
                        margin-bottom: 0em;
                    }
                    h4 {
                        margin: 0em;
                        padding: 0em;
                        border-bottom: 2px solid white;
                    }
                    h6 {
                        font-size: 1.1em;
                        color: #99a;
                        margin: 0.5em 0em 0.25em 0em;
                    }
                    dd {
                        margin-left: 1em;
                    }
                    tt {
                        font-size: 1.2em;
                    }
                    table {
                        margin-bottom: 0.5em;
                    }
                    th {
                        text-align: left;
                        font-weight: normal;
                        color: black;
                        border-bottom: 1px solid black;
                        padding: 3px 6px;
                    }
                    td {
                        padding: 3px 6px;
                        vertical-align: top;
                        background-color: f6f6ff;
                        font-size: 0.85em;
                    }
                    td p {
                        margin: 0px;
                    }
                    ul {
                        padding-left: 1.75em;
                    }
                    p + ul, p + ol, p + dl {
                        margin-top: 0em;
                    }
                    .optional {
                        font-weight: normal;
                        opacity: 0.75;
                    }
                    .code-tag {
                         color: #000091;
                        background-color: inherit;
                    }
                    .code-comment {
                         color: #808080;
                        background-color: inherit;
                    }
                    .code-xml .code-keyword {
                        color: inherit;
                        font-weight: bold;
                    }
                    .code-quote {
                         color: #009100;
                        background-color: inherit;
                    }
                </style>
                <xsl:text disable-output-escaping="yes">
                <![CDATA[
                    <script type="text/javascript" src="json2.js" ></script>

                    <script type="text/javascript">
                        /*
                            Developed by Robert Nyman, http://www.robertnyman.com
                            Code/licensing: http://code.google.com/p/getelementsbyclassname/
                        */
                        var getElementsByClassName = function (className, tag, elm){
                            if (document.getElementsByClassName) {
                                getElementsByClassName = function (className, tag, elm) {
                                    elm = elm || document;
                                    var elements = elm.getElementsByClassName(className),
                                        nodeName = (tag)? new RegExp("\\b" + tag + "\\b", "i") : null,
                                        returnElements = [],
                                        current;
                                    for(var i=0, il=elements.length; i<il; i+=1){
                                        current = elements[i];
                                        if(!nodeName || nodeName.test(current.nodeName)) {
                                            returnElements.push(current);
                                        }
                                    }
                                    return returnElements;
                                };
                            }
                            else if (document.evaluate) {
                                getElementsByClassName = function (className, tag, elm) {
                                    tag = tag || "*";
                                    elm = elm || document;
                                    var classes = className.split(" "),
                                        classesToCheck = "",
                                        xhtmlNamespace = "http://www.w3.org/1999/xhtml",
                                        namespaceResolver = (document.documentElement.namespaceURI === xhtmlNamespace)? xhtmlNamespace : null,
                                        returnElements = [],
                                        elements,
                                        node;
                                    for(var j=0, jl=classes.length; j<jl; j+=1){
                                        classesToCheck += "[contains(concat(' ', @class, ' '), ' " + classes[j] + " ')]";
                                    }
                                    try	{
                                        elements = document.evaluate(".//" + tag + classesToCheck, elm, namespaceResolver, 0, null);
                                    }
                                    catch (e) {
                                        elements = document.evaluate(".//" + tag + classesToCheck, elm, null, 0, null);
                                    }
                                    while ((node = elements.iterateNext())) {
                                        returnElements.push(node);
                                    }
                                    return returnElements;
                                };
                            }
                            else {
                                getElementsByClassName = function (className, tag, elm) {
                                    tag = tag || "*";
                                    elm = elm || document;
                                    var classes = className.split(" "),
                                        classesToCheck = [],
                                        elements = (tag === "*" && elm.all)? elm.all : elm.getElementsByTagName(tag),
                                        current,
                                        returnElements = [],
                                        match;
                                    for(var k=0, kl=classes.length; k<kl; k+=1){
                                        classesToCheck.push(new RegExp("(^|\\s)" + classes[k] + "(\\s|$)"));
                                    }
                                    for(var l=0, ll=elements.length; l<ll; l+=1){
                                        current = elements[l];
                                        match = false;
                                        for(var m=0, ml=classesToCheck.length; m<ml; m+=1){
                                            match = classesToCheck[m].test(current.className);
                                            if (!match) {
                                                break;
                                            }
                                        }
                                        if (match) {
                                            returnElements.push(current);
                                        }
                                    }
                                    return returnElements;
                                };
                            }
                            return getElementsByClassName(className, tag, elm);
                        };

                        function toggle(id) {
                            var el = document.getElementById(id);
                            el.style.display = (el.style.display != 'none' ? 'none' : '' );
                            document.getElementById("link-" + id).innerHTML = (el.style.display == 'none' ? 'expand' : 'collapse' );
                        }

                        function hideAll() {
                            var examples = getElementsByClassName("toggle", "div");
                            for (var i = 0; i < examples.length; i++) {
                                toggle(examples[i].id);
                            }
                        }

                        function format(codeElem) {
			                try {
                                var obj = JSON.parse(codeElem.innerHTML);
                                codeElem.innerHTML = JSON.stringify(obj, null, 4);
                            }
                            catch (e)
                            {
                                // err
                            }
                        }
                        
                        function formatAll() {
                            var jsonCodes = document.getElementsByTagName("code");
                            for (var i = 0; i < jsonCodes.length; i++) {
                                format(jsonCodes[i]);
                            }
                        }
                    </script>
                ]]>
                </xsl:text>

            </head>
            <body onload="hideAll(); formatAll();">
                <h1>
                    <xsl:choose>
                        <xsl:when test="wadl:doc[@title]">
                            <xsl:value-of select="wadl:doc[@title][1]/@title"/>
                        </xsl:when>
                        <xsl:otherwise>My Web Application</xsl:otherwise>
                    </xsl:choose>
                </h1>

                <p>This document describes the REST API and resources provided by JIRA.
                    The REST APIs are for developers who want to integrate JIRA into their application
                    and for administrators who want to script interactions with the JIRA server.
                </p>

                <p>JIRA's REST APIs provide access to resources (data entities) via URI paths.
                    To use a REST API, your application will make an HTTP request and parse the response. Currently, the only supported reponse format is JSON.
                    Your methods will be the standard HTTP methods like GET, PUT, POST and DELETE
                    (see API descriptions below for which methods are available for each resource). 
                </p>

                <p>Because the REST API is based on open standards, you can use any web development language to access the API.
                </p>

                <h2><a name="JIRA4.2RESTAPIdiscussionpoints-StructureoftheRESTURIs"></a>Structure of the REST URIs
                </h2>

                <p>URIs for JIRA's REST API resource have the following structure:</p>

                <p>With context:
                    <a href="http://host:port/context/rest/api-name/api-version/resource-name" class="external-link"
                       rel="nofollow">http://host:port/context/rest/api-name/api-version/resource-name
                    </a>
                </p>

                <p>Or without context:
                    <a href="http://host:port/rest/api-name/api-version/resource-name" class="external-link"
                       rel="nofollow">http://host:port/rest/api-name/api-version/resource-name
                    </a>
                </p>

                <p>Currently, the are two api-names available 'api' and 'auth'. REST endpoints in the 'api' path allow you to access most of the information contained within an issue. The current api-version is 2.0.alpha1.
                    REST endpoints in the 'auth' path allow you to access information related to authentication. The current api-version is 1.
                </p>

                <p>Example with context:
                    <a href="http://myhost.com:8080/jira/rest/api/2.0.alpha1/project/JRA" class="external-link"
                       rel="nofollow">http://myhost.com:8080/jira/rest/api/2.0.alpha1/project/JRA
                    </a>
                </p>

                <p>Example without context:
                    <a href="http://myhost.com:8080/rest/api/2.0/project/JRA" class="external-link"
                       rel="nofollow">http://myhost.com:8080/rest/api/2.0.alpha1/project/JRA
                    </a>
                </p>

                <h2><a name="JIRA4.2RESTAPIdiscussionpoints-HowtouseexpansionintheRESTAPIs"></a>How to use
                    expansion in the REST APIs
                </h2>

                <p>In order to minimise network traffic from the client perspective, our API uses a technique called expansion.
                </p>

                <p>You can use the 'expand' query parameter to specify a comma-separated list of entities that you want
                    expanded, identifying each entity by a given identifier. For example, the value "comments,worklogs"
                    requests the expansion of entities for which the expand identifier is "comments" and worklogs".
                </p>

                <p>To discover the identifiers for each entity, look at the 'expand' attribute in the parent object. In
                    the JSON example below, the object declares widgets as being expandable.
                </p>

                <p>Note: The 'expand' attribute should not be confused with the 'expand' query parameter which specifies
                    which entities you want expanded.
                </p>

                <p>You can use the dot notation to specify expansion of entities within another entity.
                    For example "children.children" would expand the children and the children's children (because its
                    expand identifier is children) and the child entities within the plugin. 
                </p>

                <p>All methods return accept and return JSON exclusively. Example:</p>
     
                <div class="methods">
                    <pre><code>{"expand":"widgets", "self":"http://www.example.com/jira/rest/api/resource/KEY-1", "widgets":{"widgets":[],"size":5}}</code></pre>
                </div>

                <xsl:apply-templates select="wadl:doc"/>
                <ul>
                    <li><a href="#resources">Resources</a>
                        <xsl:apply-templates select="exsl:node-set($resources)" mode="toc"/>
                    </li>
                    <xsl:if test="descendant::wadl:fault">
                        <li><a href="#faults">Faults</a>
                            <ul>
                                <xsl:apply-templates select="exsl:node-set($resources)/descendant::wadl:fault" mode="toc"/>
                            </ul>
                        </li>
                    </xsl:if>
                </ul>
                <h2 id="resources">Resources</h2>
               
                   <xsl:apply-templates select="exsl:node-set($resources)" mode="list"/>
                <xsl:if test="exsl:node-set($resources)/descendant::wadl:fault"><h2 id="faults">Faults</h2>
                    <xsl:apply-templates select="exsl:node-set($resources)/descendant::wadl:fault" mode="list"/> 
                </xsl:if>
            </body>
        </html>
    </xsl:template>

    <!-- Table of Contents -->

    <xsl:template match="wadl:resources" mode="toc">
        <ul>
            <xsl:apply-templates select="wadl:resource" mode="toc">
                <xsl:with-param name="context">
                    <xsl:call-template name="trimSlashes">
                        <xsl:with-param name="path" select="@base"/>
                    </xsl:call-template>
                </xsl:with-param>
                <xsl:with-param name="depth">0</xsl:with-param>
            </xsl:apply-templates>
        </ul>
    </xsl:template>

    <xsl:template match="wadl:resource" mode="toc">
        <xsl:param name="depth"/>
        <xsl:param name="context"/>
        <xsl:variable name="name"><xsl:value-of select="$context"/>/<xsl:call-template name="trimSlashes"><xsl:with-param
                name="path" select="@path"/></xsl:call-template></xsl:variable>

        <xsl:variable name="path" select="substring-after($name, ancestor-or-self::wadl:resources/@base)"/>
        <div id="{$path}"/>
        <xsl:if test="$depth = 0">
            <xsl:text disable-output-escaping="yes">
                <![CDATA[&nbsp;]]>
            </xsl:text>
        </xsl:if>
        <li><a href="#{generate-id()}"><xsl:value-of select="$name"/></a>
            <xsl:if test="wadl:method">
                [<xsl:for-each select="wadl:method">
                    <a href="#{generate-id()}"><xsl:value-of select="@name"/></a>
                    <xsl:if test="position() != last()">, </xsl:if>
                </xsl:for-each>]
            </xsl:if>
        <xsl:if test="wadl:resource">
            <ul>
                <xsl:apply-templates select="wadl:resource" mode="toc">
                    <xsl:with-param name="context" select="$name"/>
                    <xsl:with-param name="depth" select="$depth + 1"/>
                </xsl:apply-templates>
            </ul>
        </xsl:if>
        </li>
    </xsl:template>

    <xsl:template match="wadl:representation|wadl:fault" mode="toc">
        <xsl:variable name="href" select="@id"/>
        <xsl:choose>
            <xsl:when test="preceding::wadl:*[@id=$href]"/>
            <xsl:otherwise>
                <li>
                    <a href="#{generate-id()}">
                        <xsl:call-template name="representation-name"/>
                    </a>
                </li>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Listings -->

    <xsl:template match="wadl:resources" mode="list">
        <xsl:variable name="base">
            <xsl:choose>
                <xsl:when test="substring(@base, string-length(@base), 1) = '/'">
                    <xsl:value-of select="substring(@base, 1, string-length(@base) - 1)"/>
                </xsl:when>
                <xsl:otherwise><xsl:value-of select="@base"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:apply-templates select="wadl:resource" mode="list"/>

    </xsl:template>

    <xsl:template match="wadl:resource[wadl:method]" mode="list">
        <xsl:variable name="context"><xsl:call-template name="trimSlashes"><xsl:with-param select="ancestor::wadl:resource/@path" name="path"/></xsl:call-template></xsl:variable>
        <xsl:variable name="href" select="@id"/>
        <xsl:choose>
            <xsl:when test="preceding::wadl:resource[@id=$href]"/>
            <xsl:otherwise>
                <xsl:variable name="name">
                    <!--This is used to generate the full url to this rest end point. If a rest end point method does not define a path only the path of the resource is applies.
                    Otherwise if the rest end defines a path, the full path is the concatonation of the rest resource path and the rest end point method. -->
                    <xsl:choose>
                        <xsl:when test="string-length($context)">
                            /<xsl:value-of select="$context"/>/<xsl:call-template name="trimSlashes"><xsl:with-param select="@path" name="path"/></xsl:call-template>
                        </xsl:when>
                        <xsl:otherwise>
                            /<xsl:call-template name="trimSlashes"><xsl:with-param select="@path" name="path"/></xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:for-each select="wadl:param[@style='matrix']">
                        <span class="optional">;<xsl:value-of select="@name"/>=...</span>
                    </xsl:for-each>
                </xsl:variable>
                <div class="resource">
                    <h3 id="{generate-id()}">
                        <xsl:choose>
                            <xsl:when test="wadl:doc[@title]"><xsl:value-of select="wadl:doc[@title][1]/@title"/></xsl:when>
                            <xsl:otherwise>
                                <xsl:copy-of select="$name"/>
                                <xsl:for-each select="wadl:method[1]/wadl:request/wadl:param[@style='query']">
                                    <xsl:choose>
                                        <xsl:when test="@required='true'">
                                            <xsl:choose>
                                                <xsl:when test="preceding-sibling::wadl:param[@style='query']">&amp;</xsl:when>
                                                <xsl:otherwise>?</xsl:otherwise>
                                            </xsl:choose>
                                            <xsl:value-of select="@name"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <span class="optional">
                                                <xsl:choose>
                                                    <xsl:when test="preceding-sibling::wadl:param[@style='query']">&amp;</xsl:when>
                                                    <xsl:otherwise>?</xsl:otherwise>
                                                </xsl:choose>
                                                <xsl:value-of select="@name"/>
                                            </span>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:for-each>
                            </xsl:otherwise>
                        </xsl:choose>

                    </h3>
                    
                    <xsl:apply-templates select="wadl:doc"/>
                    
                    
                    <xsl:apply-templates select="." mode="param-group">
                        <xsl:with-param name="prefix">resource-wide</xsl:with-param>
                        <xsl:with-param name="style">template</xsl:with-param>
                    </xsl:apply-templates>
                    
                    
                    <xsl:apply-templates select="." mode="param-group">
                        <xsl:with-param name="prefix">resource-wide</xsl:with-param>
                        <xsl:with-param name="style">matrix</xsl:with-param>
                    </xsl:apply-templates>
                    <h6>Methods</h6>
                    <div class="methods">
                        <xsl:apply-templates select="wadl:method"/>
                    </div>
                </div>
                <xsl:apply-templates select="wadl:resource" mode="list">
                </xsl:apply-templates>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template> 

    <xsl:template match="wadl:method">
        <div class="method">
            <h4 id="{generate-id()}"><xsl:value-of select="@name"/></h4>
            <xsl:apply-templates select="wadl:doc"/>
            <xsl:apply-templates select="wadl:request"/>
            <xsl:apply-templates select="wadl:response"/>
        </div>
    </xsl:template>

    <xsl:template match="wadl:request">
        <xsl:apply-templates select="." mode="param-group">
            <xsl:with-param name="prefix">request</xsl:with-param>
            <xsl:with-param name="style">query</xsl:with-param>
        </xsl:apply-templates>
        <xsl:apply-templates select="." mode="param-group">
            <xsl:with-param name="prefix">request</xsl:with-param>
            <xsl:with-param name="style">header</xsl:with-param>
        </xsl:apply-templates>
        <xsl:if test="wadl:representation">
            <p><em>acceptable request representations:</em></p>
            <ul>
                <xsl:apply-templates select="wadl:representation"/>
            </ul>
        </xsl:if>
    </xsl:template>

    <xsl:template match="wadl:response">
        <xsl:apply-templates select="." mode="param-group">
            <xsl:with-param name="prefix">response</xsl:with-param>
            <xsl:with-param name="style">header</xsl:with-param>
        </xsl:apply-templates>
          <xsl:if test="wadl:representation">
            <p><em>available response representations:</em></p>
            <ul>
                <xsl:apply-templates select="wadl:representation"/>
            </ul>
        </xsl:if>
        <xsl:if test="wadl:fault">
            <p><em>potential faults:</em></p>
            <ul>
                <xsl:apply-templates select="wadl:fault"/>
            </ul>
        </xsl:if>
    </xsl:template>

    <xsl:template match="wadl:representation|wadl:fault">
        <li>
            <div>
                <xsl:call-template name="representation-name"/>
                [<a onclick="toggle('{generate-id()}'); return false;" href="#"><em><span id="link-{generate-id()}">expand</span></em></a>]
            </div>
            <div class="toggle" id="{generate-id()}">
                <xsl:apply-templates select="." mode="list" />
            </div>
        </li>
    </xsl:template>

    <xsl:template match="wadl:representation|wadl:fault" mode="list">
        <xsl:variable name="href" select="@id"/>
        <xsl:variable name="expanded-name">
            <xsl:call-template name="expand-qname">
                <xsl:with-param select="@element" name="qname"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="preceding::wadl:*[@id=$href]"/> <!-- this line is problematic for java transformers, it seems related to http://issues.apache.org/jira/browse/XALANJ-2155
 but I have no confrimed this -->
            <xsl:otherwise>
                <xsl:apply-templates select="wadl:doc"/>
                <xsl:if test="@element or wadl:param">
                    <div class="representation">
                        <!--<xsl:if test="@element">-->
                            <!--<h6>XML Schema</h6>-->
                            <!--<xsl:call-template name="get-element">-->
                                <!--<xsl:with-param name="context" select="."/>-->
                                <!--<xsl:with-param name="qname" select="@element"/>-->
                            <!--</xsl:call-template>-->
                        <!--</xsl:if>-->
                        <xsl:apply-templates select="." mode="param-group">
                            <xsl:with-param name="style">plain</xsl:with-param>
                        </xsl:apply-templates>
                        <xsl:apply-templates select="." mode="param-group">
                            <xsl:with-param name="style">header</xsl:with-param>
                        </xsl:apply-templates>
                    </div>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="wadl:*" mode="param-group">
        <xsl:param name="style"/>
        <xsl:param name="prefix"></xsl:param>
        <xsl:if test="ancestor-or-self::wadl:*/wadl:param[@style=$style]">
        <h6><xsl:value-of select="$prefix"/><xsl:text> </xsl:text><xsl:value-of select="$style"/> parameters</h6>
        <table>
            <tr>
                <th>parameter</th>
                <th>value</th>
                <th>description</th>
           </tr>
            <xsl:apply-templates select="ancestor-or-self::wadl:*/wadl:param[@style=$style]"/>
        </table>
        </xsl:if>
    </xsl:template>

    <xsl:template match="wadl:param">
        <tr>
            <td>
                <p><strong><xsl:value-of select="@name"/></strong></p>
            </td>
            <td>
                <p>
                <em><xsl:call-template name="link-qname"><xsl:with-param name="qname" select="@type"/></xsl:call-template></em>
                    <xsl:if test="@required='true'"> <small> (required)</small></xsl:if>
                    <xsl:if test="@repeating='true'"> <small> (repeating)</small></xsl:if>
                </p>
                <xsl:choose>
                    <xsl:when test="wadl:option">
                        <p><em>One of:</em></p>
                        <ul>
                            <xsl:apply-templates select="wadl:option"/>
                        </ul>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:if test="@default"><p>Default: <tt><xsl:value-of select="@default"/></tt></p></xsl:if>
                        <xsl:if test="@fixed"><p>Fixed: <tt><xsl:value-of select="@fixed"/></tt></p></xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <td>
                <xsl:apply-templates select="wadl:doc"/>
                <xsl:if test="wadl:option[wadl:doc]">
                    <dl>
                        <xsl:apply-templates select="wadl:option" mode="option-doc"/>
                    </dl>
                </xsl:if>
                <xsl:if test="@path">
                    <ul>
                        <li>XPath to value: <tt><xsl:value-of select="@path"/></tt></li>
                        <xsl:apply-templates select="wadl:link"/>
                    </ul>
                </xsl:if>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="wadl:link">
        <li>
            Link: <a href="#{@resource_type}"><xsl:value-of select="@rel"/></a>
        </li>
    </xsl:template>

    <xsl:template match="wadl:option">
        <li>
            <tt><xsl:value-of select="@value"/></tt>
            <xsl:if test="ancestor::wadl:param[1]/@default=@value"> <small> (default)</small></xsl:if>
        </li>
    </xsl:template>

    <xsl:template match="wadl:option" mode="option-doc">
            <dt>
                <tt><xsl:value-of select="@value"/></tt>
                <xsl:if test="ancestor::wadl:param[1]/@default=@value"> <small> (default)</small></xsl:if>
            </dt>
            <dd>
                <xsl:apply-templates select="wadl:doc"/>
            </dd>
    </xsl:template>

    <xsl:template match="wadl:doc">
        <xsl:param name="inline">0</xsl:param>
        <xsl:choose>
            <xsl:when test="node()[1]=text() and $inline=0">
                <p>
                    <xsl:value-of select="node()" disable-output-escaping="yes" />
                    <!--<xsl:apply-templates select="node()" mode="copy"/>-->
                </p>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="node()" mode="copy"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- utilities -->

    <xsl:template name="trimSlashes">
        <xsl:param name="path"/>

        <xsl:variable name="tmp">
            <xsl:choose>
                <xsl:when test="substring($path, string-length($path), 1) = '/'">
                    <xsl:value-of select="substring($path, 1, string-length($path) - 1)"/>
                </xsl:when>
                <xsl:otherwise><xsl:value-of select="$path"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="substring($tmp, 1, 1) = '/'">
                <xsl:value-of select="substring($tmp, 2, string-length($tmp) - 1)"/>
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="$tmp"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="get-namespace-uri">
        <xsl:param name="context" select="."/>
        <xsl:param name="qname"/>
        <xsl:variable name="prefix" select="substring-before($qname,':')"/>
        <xsl:variable name="qname-ns-uri" select="$context/namespace::*[name()=$prefix]"/>
        <!-- nasty hack to get around libxsl's refusal to copy all namespace nodes when pushing nodesets around -->
        <xsl:choose>
            <xsl:when test="$qname-ns-uri">
                <xsl:value-of select="$qname-ns-uri"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="exsl:node-set($resources)/*[1]/attribute::*[namespace-uri()='urn:namespace' and local-name()=$prefix]"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="get-element">
        <xsl:param name="context" select="."/>
        <xsl:param name="qname"/>
        <xsl:variable name="localname">
            <xsl:choose>
                <xsl:when test="contains($qname, ':')">
                    <xsl:value-of select="substring-after($qname, ':')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$qname"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:call-template name="print-type-definition">
            <xsl:with-param name="localname" select="$localname"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="print-type-definition">
        <xsl:param name="localname"/>
        <xsl:choose>
            <xsl:when test="exsl:node-set($grammars)/wadl:include/xs:schema/xs:element[@name=$localname]">
                <xsl:call-template name="print-source-link">
                  <xsl:with-param name="node" select="exsl:node-set($grammars)/wadl:include/xs:schema/xs:element[@name=$localname]"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="print-source-link">
                <xsl:with-param name="node" select="exsl:node-set($grammars)/wadl:include/xs:schema/xs:complexType[@name=$localname]"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="print-source-link">
        <xsl:param name="node"/>
        <xsl:variable name='source' select="$node/ancestor-or-self::wadl:include[1]/@href"/>
        <p><em>Source: <a href="{$source}"><xsl:value-of select="$source"/></a></em></p>
        <pre><xsl:apply-templates select="$node" mode="encode"/></pre>
    </xsl:template>

    <xsl:template name="link-qname">
        <xsl:param name="context" select="."/>
        <xsl:param name="qname"/>
        <xsl:variable name="ns-uri">
            <xsl:call-template name="get-namespace-uri">
                <xsl:with-param name="context" select="$context"/>
                <xsl:with-param name="qname" select="$qname"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="localname" select="substring-after($qname, ':')"/>
        <xsl:choose>
            <xsl:when test="$ns-uri='http://www.w3.org/2001/XMLSchema'">
                <a href="http://www.w3.org/TR/xmlschema-2/#{$localname}"><xsl:value-of select="$localname"/></a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="definition" select="exsl:node-set($grammars)/descendant::xs:*[@name=$localname][ancestor-or-self::*[@targetNamespace=$ns-uri]]"/>
                <a href="{$definition/ancestor-or-self::wadl:include[1]/@href}" title="{$definition/descendant::xs:documentation/descendant::text()}"><xsl:value-of select="$localname"/></a>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="expand-qname">
        <xsl:param name="context" select="."/>
        <xsl:param name="qname"/>
        <xsl:variable name="ns-uri">
            <xsl:call-template name="get-namespace-uri">
                <xsl:with-param name="context" select="$context"/>
                <xsl:with-param name="qname" select="$qname"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:text>{</xsl:text>
        <xsl:value-of select="$ns-uri"/>
        <xsl:text>} </xsl:text>
        <xsl:value-of select="substring-after($qname, ':')"/>
    </xsl:template>


    <xsl:template name="representation-name">
        <xsl:variable name="expanded-name">
            <xsl:call-template name="expand-qname">
                <xsl:with-param select="@element" name="qname"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="wadl:doc[@title]">
                <xsl:value-of select="wadl:doc[@title][1]/@title"/>
                <xsl:if test="@status or @mediaType or @element"> (</xsl:if>
                <xsl:if test="@status">Status Code </xsl:if><xsl:value-of select="@status"/>
                <xsl:if test="@status and @mediaType"> - </xsl:if>
                <xsl:value-of select="@mediaType"/>
                <xsl:if test="(@status or @mediaType) and @element"> - </xsl:if>
                <xsl:if test="@element">
                    <abbr title="{$expanded-name}"><xsl:value-of select="@element"/></abbr>
                </xsl:if>
                <xsl:if test="@status or @mediaType or @element">)</xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="@status"/>
                <xsl:if test="@status and @mediaType"> - </xsl:if>
                <xsl:value-of select="@mediaType"/>
                <xsl:if test="@element"> (</xsl:if>
                <abbr title="{$expanded-name}"><xsl:value-of select="@element"/></abbr>
                <xsl:if test="@element">)</xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- entity-encode markup for display -->

    <xsl:template match="*" mode="encode">
        <xsl:text>&lt;</xsl:text>
        <xsl:value-of select="name()"/><xsl:apply-templates select="attribute::*" mode="encode"/>
        <xsl:choose>
            <xsl:when test="*|text()">
                <xsl:text>&gt;</xsl:text>
                <xsl:apply-templates select="*|text()" mode="encode" xml:space="preserve"/>
                <xsl:text>&lt;/</xsl:text><xsl:value-of select="name()"/><xsl:text>&gt;</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>/&gt;</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="@*" mode="encode">
        <xsl:text> </xsl:text><xsl:value-of select="name()"/><xsl:text>="</xsl:text><xsl:value-of select="."/><xsl:text>"</xsl:text>
    </xsl:template>

    <xsl:template match="text()" mode="encode">
        <xsl:value-of select="." xml:space="preserve"/>
    </xsl:template>

    <!-- copy HTML for display -->

    <xsl:template match="html:*" mode="copy">
        <!-- remove the prefix on HTML elements -->
        <xsl:element name="{local-name()}">
            <xsl:for-each select="@*">
                <xsl:attribute name="{local-name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:apply-templates select="node()" mode="copy"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@*|node()[namespace-uri()!='http://www.w3.org/1999/xhtml']" mode="copy">
        <!-- everything else goes straight through -->
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" mode="copy"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
