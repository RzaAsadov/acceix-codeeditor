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

import org.acceix.frontend.crud.loaders.ScriptLoader;
import org.acceix.frontend.crud.models.CrudElemental;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.acceix.ide.NFileExplorer;


public class NCodeScripts extends org.acceix.frontend.helpers.ModuleHelper{
    
    
    public final static int DATATYPE_STRING = 0;
    public final static int DATATYPE_LONG = 1;
    public final static int DATATYPE_BOOLEAN = 2;
    public final static int DATATYPE_OBJECT = 3;
    public final static int DATATYPE_LIST = 4; 
    
    public final static int ERROR_SCRIPT_NOT_FOUND=-1;
    public final static int ERROR_SCRIPT_IO=-2;
    public final static int ERROR_SCRIPT_NAME_FORMAT=-3;
    public final static int ERROR_SCRIPT_ALREADY_EXIST=-4;
    public final static int SUCCESS=0;    
    
    @Override
    public void construct() {
        
        setModuleName("scripts");
        addAction(new ActionSettings("read", true,this::read));    
        addAction(new ActionSettings("delete", true,this::delete));   
        addAction(new ActionSettings("create", true,this::create)); 
        addAction(new ActionSettings("getcreatemodel", true,this::getCreateModel));
        
        addAction(new ActionSettings("createCatalog",true,this::createCatalog));  
        addAction(new ActionSettings("deleteCatalog",true,this::deleteCatalog)); 
        
        addAction(new ActionSettings("edit", true,this::edit));
        addAction(new ActionSettings("save", true,this::save));  
        
        
    }
    
        public ModuleHelper getInstance() {
            return new NCodeScripts();
        }    
    
    public List<Object[]> getFieldsList () {

        List<Object[]> fieldsList = new LinkedList<>();
        
        fieldsList.add(new Object[] {"name","Script name",DATATYPE_STRING,DATATYPE_STRING});

        return fieldsList;
        
    }    
    
    public void getCreateModel () {
        
        
        Map<Integer, Object> fieldsMap = new LinkedHashMap<>();
        
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
                    
                fieldsMap.put(fieldsMap.size(), field);
            } 
            
            addToDataModel("fields", fieldsMap); 
        
        String catalog = (String)getParameter("catalog");
        
        addToDataModel("catalog", catalog);         
        
        addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','&catalog=" + catalog +"')");
        
        addToDataModel("submit_to_module", getModuleName());
        addToDataModel("submit_to_action", "create");  

        
        addToDataModel("fields", fieldsMap);
        
        try {
            renderData("/development/dev_Createscript");
        } catch (IOException ex) {
            Logger.getLogger(NCodeScripts.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }    
    
    public void createCatalog() {
        
        String catalog = (String)getParameter("catalog");
       
        
        int resultCode = new NFileExplorer(getGlobalEnvs(),getUsername()).createCatalog("scripts", catalog);

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
       
        
        int resultCode = new NFileExplorer(getGlobalEnvs(),getUsername()).deleteCatalog("scripts", catalog);

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
        
        String scriptName = (String)getParameter("name");
        String catalog = (String)getParameter("catalog");        
        
        int resultCode = createInSystem(scriptName,getGlobalEnvs().get("scripts_path") + "/" + catalog,null,getRequestObject().getParams());

        switch (resultCode) {
            case ERROR_SCRIPT_NAME_FORMAT:
                addToDataModel("result", "error");
                addToDataModel("errorCode", "1001");
                addToDataModel("message", "Wrong format of script name !");
                renderData();
                break;
            case ERROR_SCRIPT_ALREADY_EXIST:
                addToDataModel("result", "error");
                addToDataModel("errorCode", "1002");
                addToDataModel("message", "Script already exists !");
                renderData();
                break;
            case SUCCESS:
                addToDataModel("message","Script '" + scriptName + "' created !");
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
        
        String path = (String)getGlobalEnvs().get(getModuleName() + "_path") + "/";
        
        ScriptLoader loader = new ScriptLoader();        
        
        CrudElemental crudScript = loader.get(path + name);

        File file = new File(crudScript.getFilepath());
        
        if (file.delete()) {
            
                loader.unload(name);
                

                addToDataModel("doafter","loadContainerQuery('scripts','read','#netondocontentbody','')");             

                addToDataModel("message","Script '" + crudScript.getName() + "' deleted !");
                addToDataModel("result", "success");            

                renderData();

        }
        
    }    
      
    private void read() {
        
                String catalog = (String)getParameterOrDefault("catalog","");
                
                String path = (String)getGlobalEnvs().get(getModuleName() + "_path") + "/";
                

                Map<String,Object> folderData = new NFileExplorer(getGlobalEnvs(),getUsername()).exploreItemByClass(getModuleName(), catalog);
                
                folderData.forEach((key,value) -> { addToDataModel(key, value); });        


                Map<Integer,Map<String,Object>> tableRows = new LinkedHashMap<>();
                
                Format dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                int index=0;
                
                NCodeButtons buttons = new NCodeButtons();
                
                List<String> headers = new LinkedList<>();
                headers.add("Script name");
                headers.add("Last modified");
                headers.add("Actions");
                
                
                File[] files = new File(path + catalog).listFiles();
                Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());                
                
                for (File file : files) {
                    
                            if (file.isDirectory()) continue;                
                
                            CrudElemental crudScript = new ScriptLoader().get(file.getAbsolutePath());

                            
                            if (crudScript.getFilepath()==null) continue;
                    
                    Map<String,Object> columns_n = new LinkedHashMap<>();
                
                    
                    columns_n.put("0",String.valueOf(index));
                    columns_n.put("1",crudScript.getName().replaceFirst(path + catalog, "").replaceFirst("/", ""));
                    
                    Date date = new Date(crudScript.getTimeModified());

                    columns_n.put("2",dateFormat.format(date));
                    
                    
                        Map<Integer,Map<String,Object>> buttonsInTable = new LinkedHashMap<>();
                        Map<String,Object> buttonContainer = new LinkedHashMap<>();
                        
                        
                        Map<String,Object> button_edit = buttons.
                                                         createButton("Edit", 
                                                                      "module=" + getModuleName() + "&action=edit&name=" + crudScript.getName().replaceFirst(path, ""), 
                                                                      "orange", 
                                                                      false, 
                                                                      "fa fa-pencil", 
                                                                      "new-page");
                      

                        Map<String,Object> button_delete = buttons.
                                                         createButton("Delete", 
                                                                      "module=" + getModuleName() + "&action=delete&name=" + crudScript.getName().replaceFirst(path, ""), 
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
                addToDataModel("pagetitle", "Scripts");
                addToDataModel("pageLength", "100");
                
                try {            
                    renderData("/development/dev_Scripts");
                } catch (IOException ex) {
                    Logger.getLogger(NCodeScripts.class.getName()).log(Level.SEVERE, null, ex);
                }
    }

    public void edit() {
            
        String name = (String)getParameter("name");
        
        String path = (String)getGlobalEnvs().get(getModuleName() + "_path");        

        String filepath = new ScriptLoader().get(path + "/" + name).getFilepath();
        
            if (filepath.contains(".")) {
               
                String[] filepath_elements = filepath.split("\\.");
                
                addToDataModel("filetype", filepath_elements[filepath_elements.length-1]);
               
            } else {
                 addToDataModel("filetype","unknown");
            }

        
        addToDataModel("name",name);
        addToDataModel("filetitle","[" + name + "]");             
 
        addToDataModel("editmode",getModuleName());       
        addToDataModel("submit_to_module", getModuleName());
        addToDataModel("submit_to_action", "save");
        
        addToDataModel("content", getContent(filepath));
        
        try {
            renderData("/development/editor/dev_Codeeditor");
        } catch (IOException ex) {
            Logger.getLogger(NCodeScripts.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }   
    
     public void save() {
        
                String objcode = (String)getParameter("code");
                String scriptname = (String)getParameter("name");
                
                int resultCode = saveInSystem(objcode, scriptname);

                    switch (resultCode) {
                        case SUCCESS:
                            addToDataModel("message","Script '" + scriptname
                                                        .replaceFirst((String)getGlobalEnvs()
                                                        .get(getModuleName() + "_path"), "")
                                                        .replaceAll("/", " / ") + "' saved !");
                            
                            addToDataModel("result", "success");
                            renderData();
                            return;
                        case ERROR_SCRIPT_NOT_FOUND:
                            addToDataModel("message","Script file not found !");
                            addToDataModel("result", "error");
                            renderData();
                            return;
                        case ERROR_SCRIPT_IO:
                            addToDataModel("message","Error on saving !");
                            addToDataModel("result", "error");
                            renderData();
                            break;
                        default:
                            break;
                    }

    }  
     
    

    public int saveInSystem (String code, String name) {

                byte[] decodedBytes = Base64.getDecoder().decode(code);
                String decodedString = new String(decodedBytes);        
                

                    try {                
                        ScriptLoader loader = new ScriptLoader();

                        File filepath = new File(loader.get((String)getGlobalEnvs().get(getModuleName() + "_path") + "/" + name).getFilepath());
                        
                                
                        if (filepath.exists()) {
                            try (FileWriter fw = new FileWriter(filepath)) {
                                fw.write(decodedString);
                                fw.flush();
                            }
                            

                            
                            loader.unload(name);
                            loader.load(filepath);

                            return SUCCESS;
                            
                        } else {
                            return ERROR_SCRIPT_NOT_FOUND;
                        }
                        
                    } catch (IOException ex) {
                        return ERROR_SCRIPT_IO;
                    }

    }  


    public int createInSystem(String name,String path,String type,Map<String,Object> params) {
        
        
        Pattern p1 = Pattern.compile("[^a-z0-9 _-]", Pattern.CASE_INSENSITIVE);
        Pattern p2 = Pattern.compile("[.]", Pattern.CASE_INSENSITIVE);
        


        if (!p1.matcher(name).find() || !p2.matcher(name).find()) {
            return ERROR_SCRIPT_NAME_FORMAT;
        }
        
        if (!path.equals("")) path = path + "/";
        
        String scriptFilePath = path + name;
        
        File file = new File(scriptFilePath);
        
        if (file.exists()) {
            return ERROR_SCRIPT_ALREADY_EXIST;            
        } else {

            try {
                if (file.createNewFile()) { 
                        new ScriptLoader().load(file);
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
