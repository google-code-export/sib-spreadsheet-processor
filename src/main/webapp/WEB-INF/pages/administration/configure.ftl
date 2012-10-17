[#ftl]
[#include "/WEB-INF/pages/include/header_configuration.ftl"]
	<div class="container">
    	<div class="content">
    	
	<div class="content-inner">
    	<h2>[@s.text name="sibsp.application.title.welcome"/]</h2>
		<h4>[@s.text name="admin.config.setup.title"/]</h4>
		<blockquote><p>[@s.text name="admin.config.setup.message"/]</p></blockquote>
		
		[@s.actionmessage/]
		[#if warnings?size>0]
			<div class="alert alert-error">	    
				<ul>
					[#list warnings as w]
						<li><span>${w!}</span></li>
					[/#list]
				</ul>
			</div>
        [/#if]
        [#if (actionErrors?exists && actionErrors?size > 0)]
        	<div class="alert alert-error">	
        		<ul>
        			[#list actionErrors as error]
        				<li><span>${error}</span></li>
        			[/#list]
        		</ul>
        	</div>
        [/#if]
		
		[@s.form action="configure.do" method="post" cssClass="form-horizontal"]
			<div class="control-group">
				<label class="control-label" for="dataDir">[@s.text name="admin.config.setup.datadir.label"/] <a href="#" rel="tooltip" data-placement="top" data-original-title="[@s.text name="admin.config.setup.datadir.label.tooltip"/]"><i class="icon-info-sign"></i></a></label>
				<div class="controls">
					[@s.textfield key="admin.config.setup.datadir" name="dataDirPath" size="80" required="true" cssStyle="width:306px;" placeholder="%{getText('admin.config.setup.datadir.label')}"/]
				</div>
			</div>
			<div class="control-group">
				<label class="control-label" for="urlPath">[@s.text name="admin.config.setup.urlpath.label"/] <a href="#" rel="tooltip" data-placement="top" data-original-title="[@s.text name="admin.config.setup.urlpath.label.tooltip"/]"><i class="icon-info-sign"></i></a></label>
				<div class="controls">
					[@s.textfield key="admin.config.setup.urlpath" name="urlPath" size="80" required="true" cssStyle="width:306px;" value="${rootURL}" placeholder="%{getText('admin.config.setup.urlpath.label')}"/]
				</div>
			</div>
			<div class="control-group">
				<div class="controls">
					<button type="submit" class="btn">[@s.text name="admin.config.setup.saveButton"/]</button>
				</div>
			</div>
		[/@s.form]
	</div>
	
	</div>
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]