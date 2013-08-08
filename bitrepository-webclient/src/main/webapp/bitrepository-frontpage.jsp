<%--
  #%L
  Bitrepository Webclient
  %%
  Copyright (C) 2010 - 2013 The State and University Library, The Royal Library and The State Archives, Denmark
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as 
  published by the Free Software Foundation, either version 2.1 of the 
  License, or (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Lesser Public License for more details.
  
  You should have received a copy of the GNU General Lesser Public 
  License along with this program.  If not, see
  <http://www.gnu.org/licenses/lgpl-2.1.html>.
  #L%
  --%>
<!DOCTYPE html>
<html>
  <head>
    <title>Bitrepository frontpage</title>
    <link href="bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
  </head>
  <body>
  
  <div id="pageMenu"></div>
  <div class="container-fluid">
    <div class="row-fluid">
      <div class="span9"> 
        <div class="span9" style="height:0px; min-height:0px"></div>
        <div class="span9"><h2>Welcome</h2></div>
        <div class="span9">
           Welcome to the Bitrepository services frontpage. <br>
           Please move on the the specific service pages. <br>
           The services are: 
           <ul>
             <li><a href="dashboard.jsp">Dashboard</a></li>
             <li><a href="alarm-service.jsp">Alarm</a></li>
             <li><a href="integrity-service.jsp">Integrity</a></li>
             <li><a href="audit-trail-service.jsp">Audit trail</a></li>
             <li><a href="status-service.jsp">Status</a></li>
           </ul>

           To perform actions on the content in the repository, use the commandline clients. 
        </div>
      </div>
    </div>
  </div>
  <script type="text/javascript" src="jquery/jquery-1.9.0.min.js"></script>
  <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
  <script type="text/javascript" src="menu.js"></script>

  <script>
        
    $(document).ready(function(){
      makeMenu("bitrepository-frontpage.jsp", "#pageMenu");
    }); 

    </script>
  </body>
</html>
