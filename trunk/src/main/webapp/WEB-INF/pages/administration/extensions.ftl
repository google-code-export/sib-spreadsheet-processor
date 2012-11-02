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
				
					<h2>[@s.text name="admin.config.extension.coreTypes"/]</h2>
					
					[@s.actionmessage/]
					[#if warnings?size>0]
					<ul class="warnMessage">
					[#list warnings as w]
						<li><span>${w!}</span></li>
					[/#list]
					</ul>
					[/#if]
					[@s.actionerror/]
			
					<p>[@s.text name="admin.config.extension.no.coreTypes.installed.help"/]</p>
					
					[#assign count=0]
					[#list extensions as extension]
					[#if extension.core]
						[#assign count=count+1]
						<a name="${extension.rowType}"></a>          
						<div class="row-without-spaces">	
							<div class="column-one">
								<div class="title">
									<div class="head">
										<a href="extension.do?id=${extension.rowType}">${extension.title}</a>
									</div>
								</div>
							</div>
							<div class="column-two">
								<div class="body">
									<div>
										<p>${extension.description!}
										[#if extension.link?has_content]<br/>[@s.text name="basic.seealso"/] <a href="${extension.link}">${extension.link}</a>[/#if]</p>             	
									</div>
									<div class="details">
										<table class="table-properties">
											<tr><td class="property">[@s.text name="extension.properties"/]</td><td>${extension.properties?size}</td></tr>
											<tr><td class="property">[@s.text name="basic.name"/]</td><td>${extension.name}</td></tr>
											<tr><td class="property">[@s.text name="basic.namespace"/]</td><td>${extension.namespace}</td></tr>
											<tr><td class="property">[@s.text name="extension.rowtype"/]</td><td>${extension.rowType}</td></tr>
											<tr><td class="property">[@s.text name="basic.keywords"/]</td><td>${extension.subject!}</td></tr>
										</table>
									</div>
								</div>
							</div>
						</div>
					[/#if]
					[/#list]
					[#if count=0]
						<p class="warn">
							[@s.text name="admin.extension.no.coreTypes.installed"/]
						</p>
					[/#if]
					
					<h2>[@s.text name="admin.config.extension.extensions"/]</h2>
					<p>[@s.text name="admin.config.extension.no.extensions.installed.help"/]</p>
					[#assign count=0]
					[#list extensions as extension]
					[#if !extension.core]
					[#assign count=count+1]
						<a name="${extension.rowType}"></a>          
						<div class="row-without-spaces">	
							<div class="column-one">
								<div class="title">
									<div class="head">
										<a href="extension.do?id=${extension.rowType}">${extension.title}</a>
									</div>
								</div>
							</div>
							<div class="column-two">
								<div class="body">
									<div>
										<p>${extension.description!}
										[#if extension.link?has_content]<br/>[@s.text name="basic.seealso"/] <a href="${extension.link}">${extension.link}</a>[/#if]</p>             	
									</div>
									<div class="details">
										<table class="table-properties">
											<tr><td class="property">[@s.text name="extension.properties"/]</td><td>${extension.properties?size}</td></tr>
											<tr><td class="property">[@s.text name="basic.name"/]</td><td>${extension.name}</td></tr>
											<tr><td class="property">[@s.text name="basic.namespace"/]</td><td>${extension.namespace}</td></tr>
											<tr><td class="property">[@s.text name="extension.rowtype"/]</td><td>${extension.rowType}</td></tr>
											<tr><td class="property">[@s.text name="basic.keywords"/]</td><td>${extension.subject!}</td></tr>
										</table>
									</div>
								</div>
							</div>
						</div>						
					[/#if]
					[/#list]
					[#if count=0]
						<p class="warn">
							[@s.text name="admin.config.extension.no.extensions.installed"/]
						</p>
					[/#if]
					
					[#if (numVocabularies>0)]
						<hr/>
						<h2>[@s.text name="extension.vocabularies.title"/]</h2>
						<p>[@s.text name="admin.config.extensions.vocabularies.update.help"/]</p>
						<p>[@s.text name="extension.vocabularies.last.update"][@s.param]${dateFormat}[/@s.param][/@s.text]</br>
							<form action='extensions.do' method='post'>
								[@s.submit name="updateVocabs" key="button.update"/]
								[@s.text name="extension.vocabularies.number"][@s.param]${numVocabularies}[/@s.param][/@s.text]
							</form>
						</p>
					[/#if]
					<hr/>
						<h2>[@s.text name="extension.further.title"/]</h2>
						<p>[@s.text name="extension.further.title.help"/]</p>
						[#assign count=0]
						[#list newExtensions as ext]
						[#assign count=count+1]
							<div class="row-without-spaces">	
								<div class="column-one">
									<div class="title">
										<div class="head">
											${ext.title}
											<div class="actions">
												<form action='extension.do' method='post'>
													<input type='hidden' name='url' value='${ext.url}' />
													[@s.submit name="install" key="button.install"/]
												</form>
											</div>
										</div>
									</div>
								</div>
								<div class="column-two">
									<div class="body">
										<div>
											<p>${ext.description!}</p>
										</div>
										<div class="details">
											<table class="table-properties">
												<tr><td class="property">[@s.text name="extension.rowtype"/]</td><td>${ext.rowType!}</td></tr>
												<tr><td class="property">[@s.text name="basic.keywords"/]</td><td>${ext.subject!}</td></tr>
											</table>
										</div>
									</div>
								</div>
							</div>
						[/#list]
						[#if count=0]
							[@s.text name="extension.already.installed"/]
						[/#if]
				</div>	
			</div>
		</div>
		
	</div>
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]