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
						<li>Download and complete the template according to workbook instructions</li>
						<li>Click "Select file" button to upload the completed spreadsheet file</li>
						<li>Receive and save a validated DwCA (Darwin Core Archive File) or EML metadata document</li>
					</ol>
		
					<h3>Available templates</h3>
					<dl class="dl-horizontal">
						<dt>Metadata file</dt>
						<dd>This template describe a database or other data resource. Processor output is an Ecological Metadata Language Document (EML) that conforms to a GBIF (GMP) metadata profile.</dd>
					</dl>
					<a class="btn btn-success" href="/sib-spreadsheet-processor/templates/GMP_template_version_1.0.xls"><i class="icon-download-alt"></i>GMP_template_version_1.0.xls (Excel 97-2003)</a>
					<a class="btn btn-success" href="/sib-spreadsheet-processor/templates/GMP_template_version_1.0.xls"><i class="icon-download-alt"></i>GMP_template_version_1.0.xlsx (Excel 2007-2013)</a>
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