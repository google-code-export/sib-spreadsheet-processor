[#ftl]
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>[@s.text name="sibsp.application.title"/]</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="The spreadsheet processor is a web tool that transform SIB Colombia preconfigured MS Excel spreadsheet files to GBIF Standard Darwin Core Archive file. One worksheet supports the GBIF metadata profile and another worksheet the biodiversity data. The spreadsheet processor process files via web-form making and makes validation and transformation to return a Darwin Core Archive file.">
    <meta name="keywords" content="sib, sib colombia, dwca, darwin core archive spreadsheet processor, excel conversion, excel, darwin core, transformation tool, darwin core transformation tool, excel to dwca translator, template">
    <meta name="author" content="Coordination Team of Biodiversity Information Systems Colombia (SIB Colombia)">

    <!-- Styles -->
    <link href="${rootURL}/css/bootstrap.css" rel="stylesheet">
    <style type="text/css">
      body {
        padding-top: 60px;
        padding-bottom: 40px;
      }
    </style>
    <link href="${rootURL}/css/bootstrap-responsive.css" rel="stylesheet">
    <link href="${rootURL}/css/style.css" rel="stylesheet">

    <!-- Code for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <!-- Fav and touch icons -->
    <link rel="shortcut icon" href="${rootURL}/ico/favicon.ico">
    <link rel="apple-touch-icon-precomposed" sizes="144x144" href="${rootURL}/ico/apple-touch-icon-144-precomposed.png">
    <link rel="apple-touch-icon-precomposed" sizes="114x114" href="${rootURL}/ico/apple-touch-icon-114-precomposed.png">
    <link rel="apple-touch-icon-precomposed" sizes="72x72" href="${rootURL}/ico/apple-touch-icon-72-precomposed.png">
    <link rel="apple-touch-icon-precomposed" href="${rootURL}/ico/apple-touch-icon-57-precomposed.png">
  </head>

  <body>

    <div class="navbar navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="/sib-spreadsheet-processor"><img src="${rootURL}/images/logo_sib_50px.png" alt="Logo SIB Colombia">Spreadsheet Processor</a>
          <div class="nav-collapse collapse">
            <ul class="nav pull-right">
              <li class="active"><a href="${rootURL}">[@s.text name="sibsp.application.home"/]</a></li>
              <li><a href="${rootURL}/administration/">[@s.text name="sibsp.application.configuration"/]</a></li>
              <li><a href="#about">[@s.text name="sibsp.application.about"/]</a></li>
              <li><a href="#contact">[@s.text name="sibsp.application.privacy"/]</a></li>
              <li><a href="#contact">[@s.text name="sibsp.application.contact"/]</a></li>
            </ul>
          </div><!--/.nav-collapse -->
        </div>
      </div>
    </div>

    