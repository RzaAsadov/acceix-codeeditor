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

import org.acceix.frontend.crud.loaders.FunctionLoader;
import org.acceix.frontend.crud.models.CrudFunction;
import org.acceix.frontend.database.AdminFunctions;
import org.acceix.frontend.helpers.ActionSettings;
import org.acceix.frontend.helpers.ModuleHelper;
import org.acceix.frontend.helpers.NCodeButtons;
import org.acceix.frontend.models.RoleModel;
import org.acceix.frontend.web.commons.DataUtils;
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
import org.acceix.ndatabaseclient.MachineDataException;
import org.acceix.ide.NFileExplorer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class NCodeFunctions extends org.acceix.frontend.helpers.ModuleHelper {
    
    FunctionLoader loader = new FunctionLoader();
    
    public static int FUNCTION_TYPE_EXECUTE = 1;
    public static int FUNCTION_TYPE_SQL = 2;
    public static int FUNCTION_TYPE_PROCEDURE = 3;
    public static int FUNCTION_TYPE_UNKNWON = 4;   
    
    public final static int DATATYPE_STRING = 0;
    public final static int DATATYPE_LONG = 1;
    public final static int DATATYPE_BOOLEAN = 2;
    public final static int DATATYPE_OBJECT = 3;
    public final static int DATATYPE_LIST = 4;
    
    public final static int ERROR_FUNCTION_NAME_CHANGED=-1;
    public final static int ERROR_FUNCTION_NAME_FORMAT=-2;
    public final static int ERROR_FUNCTION_NOT_FOUND=-3;
    public final static int ERROR_FUNCTION_PARSE=-4;
    public final static int ERROR_FUNCTION_IO=-5; 
    public final static int ERROR_FUNCTION_ALREADY_EXISTS=-6; 
    public final static int SUCCESS=0;    
    
    @Override
    public void construct() {
        
        setModuleName("functions");
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
            return new NCodeFunctions();
        }    
    
    public List<Object[]> getFieldsList () {

        
        List<Object[]> fieldsList = new LinkedList<>();
        
        Map<String,String> operationType = new LinkedHashMap<>();
        
        operationType.put("create", "create");
        operationType.put("read", "read");
        operationType.put("update", "update");
        operationType.put("delete", "delete");
        operationType.put("function", "function");
        operationType.put("procedure", "procedure");
        operationType.put("executable", "executable");
        
        Map<String,String> rolesList = new LinkedHashMap<>();

        
        try {
            new AdminFunctions(getGlobalEnvs(),getUsername()).getAllRoleList().forEach( (RoleModel rolemodel)-> {
                rolesList.put(rolemodel.getRolename(), rolemodel.getRoledesc());
            });
        } catch (MachineDataException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        

        
        fieldsList.add(new Object[] {"name","Function name",DATATYPE_STRING,DATATYPE_STRING});
        fieldsList.add(new Object[] {"title","Function title",DATATYPE_STRING,DATATYPE_STRING});
        fieldsList.add(new Object[] {"operationType","Operation type",DATATYPE_LIST,DATATYPE_STRING,operationType});
        fieldsList.add(new Object[] {"roleRun","Role for execute",DATATYPE_LIST,DATATYPE_STRING,rolesList});
        fieldsList.add(new Object[] {"requireAuth","Require auth",DATATYPE_BOOLEAN,DATATYPE_BOOLEAN});
   
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
            renderData("/development/dev_Createfunction");
        } catch (IOException ex) {
            Logger.getLogger(NCodeFunctions.class.getName()).log(Level.SEVERE, null, ex);
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
        
        String functionName = (String)getParameter("name");
        String functionType = (String)getParameter("functiontype");
        String catalog = (String)getParameter("catalog"); 
        
        
        int resultCode = createInSystem(functionName,catalog, functionType,getRequestObject().getParams());
        

        switch (resultCode) {
            case ERROR_FUNCTION_NAME_FORMAT:
                addToDataModel("result", "error");
                addToDataModel("errorCode", "1001");
                addToDataModel("message", "Wrong format of function name !");
                renderData();
                return;
            case ERROR_FUNCTION_ALREADY_EXISTS:
                addToDataModel("result", "error");
                addToDataModel("errorCode", "1002");
                addToDataModel("message", "Function already exists !");
                renderData();
                return;
            case SUCCESS:
                addToDataModel("message","Function '" + functionName + "' created !");
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
        
        CrudFunction crudFunction = loader.get(name);

        File f = new File(crudFunction.getFilepath());
        
        if (f.delete()) {
            
                loader.unload(name);

                addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','')");             

                addToDataModel("message","Function '" + crudFunction.getName() + "' deleted !");
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
                headers.add("Func. name");
                headers.add("Func. title");
                headers.add("Func. inputs");
                headers.add("Last modified");
                headers.add("Actions");
                
                File[] files = new File(getGlobalEnvs().get(getModuleName() + "_path").toString() + "/" + catalog).listFiles();
                Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());                
                
                for (File file : files) {
                    
                        if (file.isDirectory()) continue;                
                
                        //System.out.println("function: " + file.getName());
                        CrudFunction crudFunction = loader.get(file.getName().split("\\.")[0]);
                        
                    if (crudFunction==null) {
                        System.out.println("Error reading function: " + file.getName());
                        continue;
                    }
                    
                    
                    if (crudFunction.getFilepath()==null) continue;
                    
                    Map<String,Object> columns_n = new LinkedHashMap<>();                    
                    
                    columns_n.put("0",String.valueOf(index));
                    columns_n.put("1",crudFunction.getName());
                    columns_n.put("2",crudFunction.getTitle());
                    columns_n.put("3","none");


                    Date date = new Date(crudFunction.getTimeModified());

       
                    
                    //columns.add(dateFormat.format(date));
                    columns_n.put("4",dateFormat.format(date));
                    
                    
                        Map<Integer,Map<String,Object>> buttonsInTable = new LinkedHashMap<>();
                        Map<String,Object> buttonContainer = new LinkedHashMap<>();
                        
                        
                        Map<String,Object> button_edit = buttons.
                                                         createButton("Edit", 
                                                                      "module=" + getModuleName() + "&action=edit&name=" + crudFunction.getName(), 
                                                                      "orange", 
                                                                      false, 
                                                                      "fa fa-pencil", 
                                                                      "new-page");
                      

                        Map<String,Object> button_delete = buttons.
                                                         createButton("Delete", 
                                                                      "module=" + getModuleName() + "&action=delete&name=" + crudFunction.getName(), 
                                                                      "red", 
                                                                      true, 
                                                                      "fa fa-trash-o", 
                                                                      "modal-xs");                        

              

                        buttonsInTable.put(1, button_edit);
                        buttonsInTable.put(2, button_delete);                    
                    
                        
                        buttonContainer.put("buttons", buttonsInTable);
                        
                    columns_n.put("5",buttonContainer);
                    
                    tableRows.put(index, columns_n);
                    index++;
                }
                

                addToDataModel("module",getModuleName());
                addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','')");
                addToDataModel("headers", headers);
                addToDataModel("data", tableRows);
                addToDataModel("pagetitle", "Functions");
                addToDataModel("pageLength", "100");
                
                try {            
                    renderData("/development/dev_Functions");
                } catch (IOException ex) {
                    Logger.getLogger(NCodeFunctions.class.getName()).log(Level.SEVERE, null, ex);
                }
    }
    
    private void readEmbed() {


                Map<Integer,Map<String,Object>> tableRows = new LinkedHashMap<>();
                
                Format dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                int index=0;
                
                NCodeButtons buttons = new NCodeButtons();
                
                List<String> headers = new LinkedList<>();
                headers.add("Func. name");
                headers.add("Func. title");
                headers.add("Func. inputs");
                headers.add("Last modified");
                headers.add("Actions");
                
                for (CrudFunction crudFunction : loader.getList()) {
                    
                    
                    if (crudFunction.getFilepath()==null) continue;
                    
                    Map<String,Object> columns_n = new LinkedHashMap<>();                    
                    
                    columns_n.put("0",String.valueOf(index));
                    columns_n.put("1",crudFunction.getName());
                    columns_n.put("2",crudFunction.getTitle());
                    columns_n.put("3","none");



                    
                    Date date = new Date(crudFunction.getTimeModified());

       
                    
                    //columns.add(dateFormat.format(date));
                    columns_n.put("4",dateFormat.format(date));
                    
                    
                        Map<Integer,Map<String,Object>> buttonsInTable = new LinkedHashMap<>();
                        Map<String,Object> buttonContainer = new LinkedHashMap<>();
                        
                        
                        Map<String,Object> button_show = buttons.
                                                         createButton("Show", 
                                                                      "module=" + getModuleName() + "&action=editfunction&name=" + crudFunction.getName(), 
                                                                      "orange", 
                                                                      false, 
                                                                      "fa fa-pencil", 
                                                                      "new-page");
                      
                      

              

                        buttonsInTable.put(1, button_show);
                    
                        
                        buttonContainer.put("buttons", buttonsInTable);
                        
                    columns_n.put("5",buttonContainer);
                    
                    tableRows.put(index, columns_n);
                    index++;
                }
                

                addToDataModel("module", getModuleName());
                addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','')");
                addToDataModel("headers", headers);
                addToDataModel("data", tableRows);
                addToDataModel("title", "Functions");
                addToDataModel("pageLength", "100");
                
                try {            
                    renderData("/development/dev_Functions");
                } catch (IOException ex) {
                    Logger.getLogger(NCodeFunctions.class.getName()).log(Level.SEVERE, null, ex);
                }
    }    

    public void edit() {
        
        
        String name = (String)getParameter("name");
        
        
        String filepath = loader.get(name).getFilepath();
        
        addToDataModel("filetype", "json");
        
        String path = (String)getGlobalEnvs().get("functions_path");
        
        addToDataModel("extension","function");
        addToDataModel("name",name);
        addToDataModel("filetitle",name.replaceFirst(path + "/", ""));        
        

        addToDataModel("editmode",getModuleName());
        
        addToDataModel("title","[" + name + "]");        
        addToDataModel("submit_to_module", "code");
        addToDataModel("submit_to_action", "save");
        
        addToDataModel("content", getContent(filepath));
        
        try {
            renderData("/development/editor/dev_Codeeditor");
        } catch (IOException ex) {
            Logger.getLogger(NCodeFunctions.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }    
    
    public void save() {
        
                String objcode = (String)getParameter("code");
                String functionname = (String)getParameter("name");
                
                int resultCode = saveInSystem(functionname, objcode);
      
                
                switch (resultCode) {
                    case ERROR_FUNCTION_NAME_FORMAT:
                        addToDataModel("result", "error");
                        addToDataModel("message", "Wrong format of function name !");
                        renderData();     
                        return;
                    case ERROR_FUNCTION_NAME_CHANGED:
                        addToDataModel("message","Function name can not be changed !");
                        addToDataModel("result", "error");
                        renderData();        
                        return;
                    case SUCCESS:
                        addToDataModel("message","Function '" + functionname + "' saved !");
                        addToDataModel("result", "success");
                        renderData();              
                        return;
                    case ERROR_FUNCTION_NOT_FOUND:
                        addToDataModel("message","Function file not found !");
                        addToDataModel("result", "error");
                        renderData();        
                        return;
                    case ERROR_FUNCTION_PARSE:
                        addToDataModel("message","Function '" + " is invalid !");
                        addToDataModel("result", "error");
                        renderData();      
                        return;
                    case ERROR_FUNCTION_IO:
                        addToDataModel("message","Error on saving !");
                        addToDataModel("result", "error");
                        renderData();
                    default:
                        break;
                }       

    }      
    
    
    public int saveInSystem(String functionname,String objcode) {
        
                
                byte[] decodedBytes = Base64.getDecoder().decode(objcode);
                String decodedString = new String(decodedBytes);        
                
                Pattern p = Pattern.compile("[^a-z0-9 _-]", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(functionname);
                boolean isPermittedSymbolsFoundInFunctionName = m.find();

                if (isPermittedSymbolsFoundInFunctionName) return ERROR_FUNCTION_NAME_FORMAT;
             
                
                JSONParser parser = new JSONParser();
                
                    try {                
                        JSONObject jSONObject = (JSONObject) parser.parse(decodedString);
                        
                        String newFunctionName = (String)jSONObject.get("name");
                        
                        File newfile;
                        
                        if (newFunctionName.equals(functionname)) {
                            newfile = new File(loader.get(functionname).getFilepath() );
                        } else { 
                            return ERROR_FUNCTION_NAME_CHANGED;
                        }                        
                        
                        String filepath = loader.get(functionname).getFilepath();
                                
                        if (newfile.exists()) {
                            try (FileWriter fw = new FileWriter(filepath)) {
                                fw.write(decodedString);
                                fw.flush();
                            }
                            
                            loader.unload(functionname);      
                            loader.load(newfile);

                            return SUCCESS;
                            
                        } else {
                            return ERROR_FUNCTION_NOT_FOUND;
                        }
                        
                        

                        
                    } catch (ParseException ex) {      
                        return ERROR_FUNCTION_PARSE;

                    } catch (IOException ex) { 
                        return ERROR_FUNCTION_IO;
                    }

    }      
    
    public int createInSystem(String functionName,String path,String functionType,Map<String,Object> params) {
        

        
        Pattern p = Pattern.compile("[^a-z0-9 _-]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(functionName);
        boolean isPermittedSymbolsFoundInObjectName = m.find();

        if (isPermittedSymbolsFoundInObjectName) {
            return ERROR_FUNCTION_NAME_FORMAT;
        }         
        
        //objectName = objectName.replace(" ", "_");
        
        if (!path.equals("")) path = path + "/";
        
        String functionFilePath = getGlobalEnvs().get("functions_path") + "/" + path + functionName + ".json";
        
        File f = new File(functionFilePath);
        
        if (f.exists()) {
            return ERROR_FUNCTION_ALREADY_EXISTS;        
        } else {

            try {
                if (f.createNewFile()) { 
                    
                    try ( FileWriter fw = new FileWriter(f)) {
                        Map dataMapForJson = new LinkedHashMap();
                        dataMapForJson.put("objectType","functionSQL");
                        dataMapForJson.put("templateForResult","/defaultTemplates/createFunctionData");

                        Map<Integer, Object> fieldsMap = getModel(0);

                        for (Map.Entry<Integer,Object> field : fieldsMap.entrySet()) {

                            Map<String,Object> fieldData = (Map<String,Object>)field.getValue();

                            String paramValue = (String) params.get((String)fieldData.get("name"));
                            if (paramValue==null) {
                                paramValue = "false";
                            } else if (paramValue.equals("on")) {
                                paramValue = "true";
                            }

                            switch ((int)fieldData.get("datatypeinjson")) {
                                case DATATYPE_OBJECT:
                                    JSONObject nObject = new JSONObject();
                                    nObject.put(paramValue, new JSONObject());
                                    dataMapForJson.put((String)fieldData.get("name"), nObject);
                                    break;
                                case DATATYPE_STRING:
                                    dataMapForJson.put((String)fieldData.get("name"), paramValue);
                                    break;
                                case DATATYPE_BOOLEAN:
                                    dataMapForJson.put((String)fieldData.get("name"), Boolean.parseBoolean(paramValue));
                                    break;
                                case DATATYPE_LONG:
                                    dataMapForJson.put((String)fieldData.get("name"), Long.parseLong(paramValue));
                                    break;                                    
                                default:
                                    break;
                            }


                        }


                        Map<String,Map<String,String>> inputs = new LinkedHashMap<>();
                        Map<String,String> inputSample = new LinkedHashMap<>();
                        inputSample.put("dataType","string");
                        inputs.put("samplefield",inputSample);
                        dataMapForJson.put("inputs",inputs);
                        
                        Map<String,String> functionSample = new LinkedHashMap<>();
                        functionSample.put("content", "");
                        dataMapForJson.put("function",functionSample);
                        
                        var dataUtils = new DataUtils();

                        fw.write(new DataUtils().beautyfyJson(dataUtils.mapToJsonString(dataMapForJson)));
                        fw.flush();
                        
                        loader.load(f);
                    
                    }

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
