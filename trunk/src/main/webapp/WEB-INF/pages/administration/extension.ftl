[#ftl]
[#include "/WEB-INF/pages/include/header_administration.ftl"]
<div class="container">
	<div class="row">
	
		<div class="span3">
			<ul class="nav nav-list sibsp-sidenav affix-top">
          		<li><a href="${rootURL}/administration/"><i class="icon-chevron-right"></i> [@s.text name="sibsp.application.administration.general"/]</a></li>
          		<li class=""><a href="#buttonGroups"><i class="icon-chevron-right"></i> [@s.text name="sibsp.application.administration.useraccounts"/]</a></li>
          		<li class="active"><a href="${rootURL}/administration/extensions.do"><i class="icon-chevron-right"></i> [@s.text name="sibsp.application.administration.coretypesandextensions"/]</a></li>
        	</ul>
		</div>
		
		<div class="span9">
			<div class="content content-full-witdh">    	
				<div class="content-inner">
				
					<h2>[@s.text name="admin.config.extension.title"/]: ${extension.title}</h2>
					        
					<div class="row-without-spaces2">	
						<div class="column-one">
							<p>[@s.text name="basic.title"/]</p>
						</div>
						<div class="column-two">
							${extension.title}
						</div>
					</div>
					
					<div class="row-without-spaces2">	
						<div class="column-one">
							<p>[@s.text name="basic.name"/]</p>
						</div>
						<div class="column-two">
							${extension.name}
						</div>
					</div>
					
					<div class="row-without-spaces2">	
						<div class="column-one">
							<p>[@s.text name="basic.namespace"/]</p>
						</div>
						<div class="column-two">
							${extension.namespace}
						</div>
					</div>
					
					<div class="row-without-spaces2">	
						<div class="column-one">
							<p>[@s.text name="extension.rowtype"/]</p>
						</div>
						<div class="column-two">
							${extension.rowType}
						</div>
					</div>
					
					<div class="row-without-spaces2">	
						<div class="column-one">
							<p>[@s.text name="basic.description"/]</p>
						</div>
						<div class="column-two">
							${extension.description}
						</div>
					</div>
					
					[#if extension.link?has_content]
					<div class="row-without-spaces2">	
						<div class="column-one">
							<p>[@s.text name="basic.description"/]</p>
						</div>
						<div class="column-two">
							${extension.description}
						</div>
					</div>
					[/#if]
					
					<hr>
					
					<h2>[@s.text name="admin.config.extension.properties"/]</h2>
					
					[#list extension.properties as p]	
					<a name="${p.qualname}"></a>
					<div class="row-without-spaces2">	
						<div class="column-one">
							<p>${p.name}</p>
						</div>
						<div class="column-two">
							[#if p.description?has_content]${p.description}<br/>[/#if]
							[#if p.link?has_content][@s.text name="basic.seealso"/] <a href="${p.link}">${p.link}</a>[/#if]
							
							[#if p.examples?has_content]
							<em>[@s.text name="basic.examples"/]</em>: ${p.examples}
							[/#if]
							
							<table class="table-properties">
								<tr><td class="property">[@s.text name="extension.prop.qname"/]</td><td>${p.qualname}</td></tr>
								<tr><td class="property">[@s.text name="basic.namespace"/]</td><td>${p.namespace}</td></tr>
								<tr><td class="property">[@s.text name="extension.prop.group"/]</td><td>${p.group!}</td></tr>
								<tr><td class="property">[@s.text name="extension.prop.type"/]</td><td>${p.type}</td></tr>
								<tr><td class="property">[@s.text name="extension.prop.required"/]</td><td>${p.required?string}</td></tr>
							</table>
						</div>
					</div>
					[/#list]
					
				</div>	
			</div>
		</div>
		
	</div>
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]