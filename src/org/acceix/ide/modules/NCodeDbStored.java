/*
 * The MIT License
 *
 * Copyright 2022 Rza Asadov (rza dot asadov at gmail dot com).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.acceix.ide.modules;

import org.acceix.frontend.crud.loaders.DbStoredLoader;
import org.acceix.frontend.crud.models.CrudDbStored;
import org.acceix.frontend.helpers.ActionSettings;
import org.acceix.frontend.helpers.ModuleHelper;
import org.acceix.frontend.helpers.NCodeButtons;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.acceix.ide.NFileExplorer;


public class NCodeDbStored extends org.acceix.frontend.helpers.ModuleHelper {
    
    public final static int DATATYPE_STRING = 0;
    public final static int DATATYPE_LONG = 1;
    public final static int DATATYPE_BOOLEAN = 2;
    public final static int DATATYPE_OBJECT = 3;
    public final static int DATATYPE_LIST = 4; 
    
    public final static int ERROR_DBSTORED_NOT_FOUND=-1;
    public final static int ERROR_DBSTORED_IO=-2;
    public final static int ERROR_DBSTORED_NAME_FORMAT=-3;
    public final static int ERROR_DBSTORED_ALREADY_EXIST=-4;
    public final static int ERROR_DBSTORED_SYNTAX=-5;
    public final static int SUCCESS=0;    
    

    
    @Override
    public void construct() {
        
        setModuleName("dbstored");
        addAction(new ActionSettings("read", true,this::read));  
        addAction(new ActionSettings("readEmbed", true,this::readEmbed));    
        
        addAction(new ActionSettings("delete", true,this::delete)); 
        addAction(new ActionSettings("create", true,this::create));
        addAction(new ActionSettings("getcreatemodel", true,this::getcreatemodel));
        
        addAction(new ActionSettings("createCatalog",true,this::createCatalog));
        addAction(new ActionSettings("deleteCatalog",true,this::deleteCatalog));
        
        addAction(new ActionSettings("edit", true,this::edit));
        addAction(new ActionSettings("save", true,this::save));        
        
    }
    
        public ModuleHelper getInstance() {
            return new NCodeDbStored();
        }    
    
    private String lastErrorMsg = "";

    public String getLastErrorMsg() {
        return lastErrorMsg;
    }

    private void setLastErrorMsg(String lastErrorMsg) {
        this.lastErrorMsg = lastErrorMsg;
    }
        
    public List<Object[]> getFieldsList () {

        
        List<Object[]> fieldsList = new LinkedList<>();
        
        fieldsList.add(new Object[] {"name","Db stored name",DATATYPE_STRING,DATATYPE_STRING});


        return fieldsList;
        
    }

    
    public  Map<Integer,Object> getModel(int type) {
    
            Map<Integer,Object> fieldsMap = new LinkedHashMap<>();
           
            int index=0;
            
            for (Object[] fieldArray : getFieldsList()) {
            
                Map<String,Object> field = new LinkedHashMap<>();
                field.put("name", fieldArray[0]);
                field.put("displayname", fieldArray[1]);
                
                switch ((int)fieldArray[2]) {
                    case DATATYPE_BOOLEAN:
                        field.put("datatype", "boolean");
                        break;
                    case DATATYPE_STRING:
                        field.put("datatype", "string");
                        break;
                    case DATATYPE_LONG:
                        field.put("datatype", "long");
                        break;
                    case DATATYPE_LIST:
                        field.put("datatype", "enum");
                        field.put("values",fieldArray[4]);
                        break;
                    default:
                        break;
                }
                field.put("datatypeinjson", fieldArray[3]);
                    
                fieldsMap.put(index, field);
                index++;
            }
            
            return fieldsMap;      

    }      
    
    
    public void getcreatemodel () {
        
        Map<Integer, Object> fieldsMap = getModel(0);
        
        
        String catalog = (String)getParameter("catalog");
        
        addToDataModel("catalog", catalog);
                
        addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','&catalog=" + catalog +"')");
        
        addToDataModel("submit_to_module", getModuleName());
        addToDataModel("submit_to_action", "create");  

        
        addToDataModel("fields", fieldsMap);
        
        try {
            renderData("/development/dev_Createdbstored");
        } catch (IOException ex) {
            Logger.getLogger(NCodeDbStored.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }     
    
    public void createCatalog() {
        
        String catalog = (String)getParameter("catalog");
       
        
        int resultCode = new NFileExplorer(getGlobalEnvs(),getUsername()).createCatalog(getModuleName(), catalog);

        if (resultCode==NFileExplorer.CATALOG_CREATED) {
                addToDataModel("result", "success");
                addToDataModel("message", "created");
                renderData();
         
        } else if (resultCode==NFileExplorer.CATALOG_EXISTS) {
                addToDataModel("result", "error");
                addToDataModel("message", "Catalog already exists");
                renderData();            
        } else if (resultCode==NFileExplorer.CATALOG_NOT_CREATED) {
                addToDataModel("result", "error");
                addToDataModel("message", "Catalog not created");
                renderData();            
        }
        
    }    
    
    public void deleteCatalog() {
        
        String catalog = (String)getParameter("catalog");
       
        
        int resultCode = new NFileExplorer(getGlobalEnvs(),getUsername()).deleteCatalog(getModuleName(), catalog);

        if (resultCode==NFileExplorer.CATALOG_DELETED) {
                addToDataModel("result", "success");
                addToDataModel("message", "deleted");
                renderData();
        } else if (resultCode==NFileExplorer.CATALOG_NOT_EXISTS) {
                addToDataModel("result", "error");
                addToDataModel("message", "Catalog not exists");
                renderData();            
        } else if (resultCode==NFileExplorer.CATALOG_NOT_DELETED) {
                addToDataModel("result", "error");
                addToDataModel("message", "Catalog not deleted");
                renderData();            
        }
        
    }        
    
    public void create() {
        
        String dbstoredName = (String)getParameter("name");
        String catalog = (String)getParameter("catalog");         
        
        
        int resultCode = createInSystem(dbstoredName,catalog,null,getRequestObject().getParams());

        switch (resultCode) {
            case ERROR_DBSTORED_NAME_FORMAT:
                addToDataModel("result", "error");
                addToDataModel("errorCode", "1001");
                addToDataModel("message", "Wrong format of db stored name !");
                renderData();
                break;
            case ERROR_DBSTORED_ALREADY_EXIST:
                addToDataModel("result", "error");
                addToDataModel("errorCode", "1002");
                addToDataModel("message", "Db stored already exists !");
                renderData();
                break;
            case SUCCESS:
                addToDataModel("message","Db stored '" + dbstoredName + "' created !");
                addToDataModel("result", "success");
                renderData();
                break;
            default:
                break;
        }
        
    }    
    
    @SuppressWarnings("unchecked")
    private void delete() {
        
        String name = (String)getParameter("name");
        
        CrudDbStored crudDbStored = new DbStoredLoader().get(name);

        File f = new File(crudDbStored.getFilepath());
        
        if (f.delete()) {
            
                new DbStoredLoader().unload(name);
                

                addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','')");             

                addToDataModel("message","Dbstored '" + crudDbStored.getName() + "' deleted !");
                addToDataModel("result", "success");            

                renderData();

        }
        
    }    
      
    private void read() {


                String catalog = (String)getParameterOrDefault("catalog","");
                
        
                Map<String,Object> folderData = new NFileExplorer(getGlobalEnvs(),getUsername()).exploreItemByClass(getModuleName(), catalog);
                
                folderData.forEach((key,value) -> { addToDataModel(key, value); });        
        
                Map<Integer,Map<String,Object>> tableRows = new LinkedHashMap<>();
                
                Format dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                int index=0;
                
                NCodeButtons buttons = new NCodeButtons();
                
                List<String> headers = new LinkedList<>();
                headers.add("DbStored. name");
                headers.add("Last modified");
                headers.add("Actions");
                
                
                File[] files = new File(getGlobalEnvs().get(getModuleName() + "_path").toString() + "/" + catalog).listFiles();
                Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());                
                
                for (File file : files) {
                    
                        if (file.isDirectory()) continue;
                                       
                            CrudDbStored crudDbStored = new DbStoredLoader().get(file.getName().split("\\.")[0]);

                            if (crudDbStored.getFilepath()==null) continue;
                    
                    Map<String,Object> columns_n = new LinkedHashMap<>();                    
                    
                    columns_n.put("0",String.valueOf(index));
                    columns_n.put("1",crudDbStored.getName());



                    
                    Date date = new Date(crudDbStored.getTimeModified());

       
                    
                    //columns.add(dateFormat.format(date));
                    columns_n.put("2",dateFormat.format(date));
                    
                    
                        Map<Integer,Map<String,Object>> buttonsInTable = new LinkedHashMap<>();
                        Map<String,Object> buttonContainer = new LinkedHashMap<>();
                        
                        
                        Map<String,Object> button_edit = buttons.
                                                         createButton("Edit", 
                                                                      "module=" + getModuleName() + "&action=edit&name=" + crudDbStored.getName(), 
                                                                      "orange", 
                                                                      false, 
                                                                      "fa fa-pencil", 
                                                                      "new-page");
                      

                        Map<String,Object> button_delete = buttons.
                                                         createButton("Delete", 
                                                                      "module=" + getModuleName() + "&action=delete&name=" + crudDbStored.getName(), 
                                                                      "red", 
                                                                      true, 
                                                                      "fa fa-trash-o", 
                                                                      "modal-xs");                        

              

                        buttonsInTable.put(1, button_edit);
                        buttonsInTable.put(2, button_delete);                    
                    
                        
                        buttonContainer.put("buttons", buttonsInTable);
                        
                    columns_n.put("3",buttonContainer);
                    
                    tableRows.put(index, columns_n);
                    index++;
                }
                

                addToDataModel("module", getModuleName());                                
                addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','')");
                addToDataModel("headers", headers);
                addToDataModel("data", tableRows);
                addToDataModel("pagetitle", "DB Stored");
                addToDataModel("pageLength", "100");
                
                try {            
                    renderData("/development/dev_Dbstored");
                } catch (IOException ex) {
                    Logger.getLogger(NCodeDbStored.class.getName()).log(Level.SEVERE, null, ex);
                }
    }
    
    private void readEmbed() {


                Map<Integer,Map<String,Object>> tableRows = new LinkedHashMap<>();
                
                Format dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                int index=0;
                
                NCodeButtons buttons = new NCodeButtons();
                
                List<String> headers = new LinkedList<>();
                headers.add("DbStored. name");
                headers.add("Last modified");
                headers.add("Actions");
                
                DbStoredLoader dbStoredLoader = new DbStoredLoader();
                
                for (CrudDbStored crudDbStored : dbStoredLoader.getList()) {
                    
                    
                    if (crudDbStored.getFilepath()==null) continue;
                    
                    Map<String,Object> columns_n = new LinkedHashMap<>();                    
                    
                    columns_n.put("0",String.valueOf(index));
                    columns_n.put("1",crudDbStored.getName());



                    
                    Date date = new Date(crudDbStored.getTimeModified());

       
                    
                    //columns.add(dateFormat.format(date));
                    columns_n.put("2",dateFormat.format(date));
                    
                    
                        Map<Integer,Map<String,Object>> buttonsInTable = new LinkedHashMap<>();
                        Map<String,Object> buttonContainer = new LinkedHashMap<>();
                        
                        
                        Map<String,Object> button_show = buttons.
                                                         createButton("Show", 
                                                                      "module=" + getModuleName() + "&action=edit&name=" + crudDbStored.getName(), 
                                                                      "orange", 
                                                                      false, 
                                                                      "fa fa-pencil", 
                                                                      "new-page");
                    

              

                        buttonsInTable.put(1, button_show);
                    
                        
                        buttonContainer.put("buttons", buttonsInTable);
                        
                    columns_n.put("3",buttonContainer);
                    
                    tableRows.put(index, columns_n);
                    index++;
                }    
    }

    
    
    public void edit() {
            
        String name = (String)getParameter("name");

        String filepath = new DbStoredLoader().get(name).getFilepath();
        
        addToDataModel("filetype", "sql");
        
        String path = (String)getGlobalEnvs().get("dbstored_path");
        
        
        addToDataModel("extension","db");
        addToDataModel("name",name);
        addToDataModel("filetitle", name.replaceFirst(path + "/", "") );
        
        addToDataModel("editmode",getModuleName());      
        addToDataModel("submit_to_module", getModuleName());
        addToDataModel("submit_to_action", "save");
        
        addToDataModel("content", getContent(filepath));
        
        try {
            renderData("/development/editor/dev_Codeeditor");
        } catch (IOException ex) {
            Logger.getLogger(NCodeDbStored.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }    
    
 
    public void save() {
        
                String objcode = (String)getParameter("code");
                String dbstoredname = (String)getParameter("name");
                
                
                int resultCode = saveInSystem(objcode, dbstoredname);
                

                    switch (resultCode) {
                        case SUCCESS:
                            addToDataModel("message","Db stored '" + dbstoredname + "' saved !");
                            addToDataModel("result", "success");
                            renderData();
                            return;
                        case ERROR_DBSTORED_NOT_FOUND:
                            addToDataModel("message","Db stored file not found !");
                            addToDataModel("result", "error");
                            renderData();
                            return;
                        case ERROR_DBSTORED_IO:
                            addToDataModel("message","IO Error on saving !");
                            addToDataModel("result", "error");
                            renderData();
                            break;
                        case ERROR_DBSTORED_SYNTAX:
                            
                            int f_index = getLastErrorMsg().indexOf(")");
                            
                            if (f_index > -1) {
                                addToDataModel("message",getLastErrorMsg().substring(f_index + 1, getLastErrorMsg().length()));
                            } else {
                                addToDataModel("message",getLastErrorMsg());
                            }
                            addToDataModel("result", "error");
                            renderData();
                            break;                            
                        default:
                            break;
                    }

    }          
    
    
    public int saveInSystem (String objcode, String dbstoredname) {

                byte[] decodedBytes = Base64.getDecoder().decode(objcode);
                String decodedString = new String(decodedBytes); 
                
                
                    try {                

                        DbStoredLoader dbStoredLoader = new DbStoredLoader();

                        File filepath = new File(dbStoredLoader.get(dbstoredname).getFilepath());
                                
                        if (filepath.exists()) {
                            try (FileWriter fw = new FileWriter(filepath)) {
                                fw.write(decodedString);
                                fw.flush();
                            }
                            
                            dbStoredLoader.unload(dbstoredname);
                            dbStoredLoader.load(filepath);
                            
                            if (dbStoredLoader.getLast_error().length() > 0) {
                                setLastErrorMsg(dbStoredLoader.getLast_error());
                                return ERROR_DBSTORED_SYNTAX;
                            } else {
                                return SUCCESS;
                            }
                            
                        } else {
                            return ERROR_DBSTORED_NOT_FOUND;
                        }
                        
                    } catch (IOException ex) {
                        return ERROR_DBSTORED_IO;
                    }

    }  


    public int createInSystem(String dbstoredName,String path,String dbstoredtype,Map<String,Object> params) {
        
        
        Pattern p = Pattern.compile("[^a-z0-9 _-]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(dbstoredName);
        boolean isPermittedSymbolsFoundInObjectName = m.find();

        if (isPermittedSymbolsFoundInObjectName) {
            return ERROR_DBSTORED_NAME_FORMAT;
        }
        
        if (!path.equals("")) path = path + "/";
        
        String dbstoredFilePath = getGlobalEnvs().get("dbstored_path") + "/" + path + dbstoredName + ".stored";
        
        File f = new File(dbstoredFilePath);
        
        if (f.exists()) {
            return ERROR_DBSTORED_ALREADY_EXIST;            
        } else {

            try {
                if (f.createNewFile()) { 
                    
                        FileWriter fw = new FileWriter(f);
                        fw.write("CREATE OR REPLACE DEFINER=CURRENT_USER FUNCTION " + dbstoredName + "(");
                        fw.write("sampleparam CHAR(128) ) RETURNS LONGTEXT CHARSET utf8mb4\n");
                        fw.write("   MODIFIES SQL DATA\n");
                        fw.write("   DETERMINISTIC\n");
                        fw.write("BEGIN\n\n");
                        fw.write("   RETURN \"somevalue\";\n");
                        fw.write("END\n");
                        fw.flush();
                        fw.close();


                        new DbStoredLoader().load(f);

                } else {
                    
                }
            } catch (IOException | NumberFormatException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
         
        return SUCCESS;
        
        
    }   
    
    
    public String getContent(String filePath) {
        
        try {
            StringBuilder contentBuilder = new StringBuilder();

            try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            }            
            return contentBuilder.toString();
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            return "ERROR: Can not get content !";
        }
    }      

    
}
