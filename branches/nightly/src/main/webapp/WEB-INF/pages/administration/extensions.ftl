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
    	
					
	
    
				</div>	
			</div>
		</div>
		
	</div>
</div> <!-- /container -->
[#include "/WEB-INF/pages/include/footer.ftl"]