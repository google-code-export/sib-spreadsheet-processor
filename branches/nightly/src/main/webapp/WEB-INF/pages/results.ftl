[#ftl]
[#include "/WEB-INF/pages/include/header.ftl"]
<div class="container">
	<div class="row">
	
		<div class="span12">
			<div class="content content-full-witdh">    	
				<div class="content-inner">
    				<h2>[@s.text name="sibsp.application.results.title"/]</h2>
					<h4>[@s.text name="sibsp.application.results.textoagradecimiento"/]</h3>
		
					<p>[@s.text name="sibsp.application.results.overview"/]</p>

					<hr/>
					<h3>[@s.text name="sibsp.application.results.availablefiles"/]</h3>
					[#if resource.lastPublished??]
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.portal.overview.published.processingdate"/]</dt>
						<dd>${resource.lastPublished?datetime?string}</dd>
					</dl>
					[#if (resource.recordsPublished>0)]
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.portal.overview.published.archive"/]</dt>
						<dd><a href="${rootURL}/archive.do?r=${resource.uniqueID}">[@s.text name="sibsp.portal.overview.published.download"/]</a>
						(${dwcaFormattedSize}) ${resource.recordsPublished} [@s.text name="sibsp.portal.overview.published.records"/]
						</dd>
					</dl>
					[/#if]
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.portal.overview.published.eml"/]</dt>
						<dd>
							<a href="${rootURL}/eml.do?r=${resource.uniqueID}">[@s.text name="sibsp.portal.overview.published.download"/]</a>
							(${emlFormattedSize})
						</dd>
					</dl>
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.portal.resource.published.rtf"/]</dt>
						<dd>
							<a href="${rootURL}/rtf.do?r=${resource.uniqueID}">[@s.text name="sibsp.portal.overview.published.download"/]</a>
							(${rtfFormattedSize})
						</dd>
					</dl>
					[/#if]
				</div>	
			</div>
		</div>
		
	</div>
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]