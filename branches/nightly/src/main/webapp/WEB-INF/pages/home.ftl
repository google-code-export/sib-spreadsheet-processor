[#ftl]
[#include "/WEB-INF/pages/include/header.ftl"]
<div class="container">
	<div class="row">
	
		<div class="span8">
			<div class="content content-full-witdh">    	
				<div class="content-inner">
    				<h2>[@s.text name="sibsp.application.title.welcome"/]</h2>
					<h3>[@s.text name="sibsp.application.instructions.title"/]</h3>
		
					<ol>
						<li>[@s.text name="sibsp.application.firststep"/]</li>
						<li>[@s.text name="sibsp.application.secondstep"/]</li>
						<li>[@s.text name="sibsp.application.thirdstep"/]</li>
						<li>[@s.text name="sibsp.application.fourthstep"/]</li>
					</ol>
					<hr/>
					<h3>[@s.text name="sibsp.application.templateslist.title"/]</h3>
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.application.metadatafile.name"/]</dt>
						<dd>[@s.text name="sibsp.application.metadatafile.description"/]<br/> <a class="btn btn-success btn-variacion" href="${rootURL}/templates/GMP_template_version_1.0.xls"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 97-2003)</a><a class="btn btn-success btn-variacion" href="${rootURL}/templates/GMP_template_version_1.0.xlsx"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 2007-2013)</a></dd>
					</dl>
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.application.ocurrence.name"/]<br/><span class="type-element">[@s.text name="sibsp.application.ocurrence.type.basic"/]</span></dt>
						<dd>[@s.text name="sibsp.application.ocurrence.minimal.description"/]<br/> <a class="btn btn-success btn-variacion" href="${rootURL}/templates/DwC_min_elements_template_version_1.0.xls"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 97-2003)</a><a class="btn btn-success btn-variacion" href="${rootURL}/templates/DwC_min_elements_template_version_1.0.xlsx"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 2007-2013)</a></dd>
					</dl>
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.application.ocurrence.name"/]<br/><span class="type-element">[@s.text name="sibsp.application.ocurrence.type.complete"/]</span></dt>
						<dd>[@s.text name="sibsp.application.ocurrence.complete.description"/]<br/> <a class="btn btn-success btn-variacion" href="${rootURL}/templates/DwC_complete_elements_template_version_1.0.xls"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 97-2003)</a><a class="btn btn-success btn-variacion" href="${rootURL}/templates/DwC_complete_elements_template_version_1.0.xlsx"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 2007-2013)</a></dd>
					</dl>
					<dl class="dl-horizontal">
						<dt>[@s.text name="sibsp.application.taxonomic.name"/]</dt>
						<dd>[@s.text name="sibsp.application.taxonomic.description"/]<br/> <a class="btn btn-success btn-variacion" href="${rootURL}/templates/DwC_taxonomic_list_template_version_1.0.xls"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 97-2003)</a><a class="btn btn-success btn-variacion" href="${rootURL}/templates/DwC_taxonomic_list_template_version_1.0.xlsx"><i class="icon-download-alt"></i>[@s.text name="button.download"/] (Excel 2007-2013)</a></dd>
					</dl>
				</div>	
			</div>
		</div>
		
		<div class="span4">
			<div class="content content-full-witdh">    	
				<div class="content-inner">
				
					<div id="new-resource">
						<h3>[@s.text name="sibsp.application.newtemplate.title"/]</h3>
						[#include "include/upload_new_template.ftl"/]
					</div>
    
				</div>	
			</div>
		</div>
		
	</div>
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]