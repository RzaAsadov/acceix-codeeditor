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

import org.acceix.frontend.crud.loaders.ObjectLoader;
import org.acceix.frontend.helpers.NCodeButtons;
import org.acceix.frontend.crud.models.CrudObject;
import org.acceix.frontend.database.AdminFunctions;
import org.acceix.frontend.helpers.ActionSettings;
import org.acceix.frontend.helpers.DbMetaData;
import org.acceix.frontend.helpers.ModuleHelper;
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
import org.acceix.ndatabaseclient.DataConnector;
import org.acceix.ndatabaseclient.MachineDataException;
import org.acceix.ide.NFileExplorer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class NCodeObjects extends org.acceix.frontend.helpers.ModuleHelper {
        
    
    public final static int OBJECT_TYPE_SIMPLE = 0;
    
    public final static int DATATYPE_STRING = 0;
    public final static int DATATYPE_LONG = 1;
    public final static int DATATYPE_BOOLEAN = 2;
    public final static int DATATYPE_OBJECT = 3;
    public final static int DATATYPE_LIST = 4;
    public final static int DATATYPE_DOUBLE = 5;
    public final static int DATATYPE_TEXT = 6;
    public final static int DATATYPE_HIDDEN = 7;
    
    public final static int ERROR_OBJECT_NAME_CHANGED=-1;
    public final static int ERROR_OBJECT_NAME_FORMAT=-2;
    public final static int ERROR_OBJECT_NOT_FOUND=-3;
    public final static int ERROR_OBJECT_PARSE=-4;
    public final static int ERROR_OBJECT_IO=-5;
    public final static int ERROR_OBJECT_ALREADY_EXIST=-6;
    
    public final static int SUCCESS=0;
    
    
    @Override
    public void construct() {
        
        setModuleName("objects");
        addAction(new ActionSettings("read", true,this::read)); 
        addAction(new ActionSettings("readEmbed", true,this::readEmbed));
        
        addAction(new ActionSettings("delete", true,this::delete)); 
        addAction(new ActionSettings("create",true,this::create));
        addAction(new ActionSettings("getcreatemodel", true,this::getCreateModel));
        
        addAction(new ActionSettings("createCatalog",true,this::createCatalog));
        addAction(new ActionSettings("deleteCatalog",true,this::deleteCatalog));
        
        addAction(new ActionSettings("edit", true,this::edit));
        addAction(new ActionSettings("save", true,this::save));        
        
    }
    
        public ModuleHelper getInstance() {
            return new NCodeObjects();
        }    
    
   public List<Object[]> getFieldsList () {

        
        List<Object[]> fieldsList = new LinkedList<>();
        
        Map<String,String> externalValues = new LinkedHashMap<>();
        Map<String,String> rolesList = new LinkedHashMap<>();
        
        List<String> tableList = new DbMetaData(new DataConnector(getGlobalEnvs(),getUsername())).getTableList();

        
        try {
            new AdminFunctions(getGlobalEnvs(),getUsername()).getAllRoleList().forEach( (RoleModel rolemodel) -> {
                rolesList.put(rolemodel.getRolename(), rolemodel.getRoledesc());
            });
        } catch (MachineDataException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        

        
        tableList.forEach((table) -> {
            externalValues.put(table, table);
        });
        
        
        
        fieldsList.add(new Object[] {"name","Object name",DATATYPE_STRING,DATATYPE_STRING,""});
        fieldsList.add(new Object[] {"title","Object title",DATATYPE_STRING,DATATYPE_STRING,""});
        fieldsList.add(new Object[] {"tables","Table",DATATYPE_LIST,DATATYPE_OBJECT,externalValues});        
        fieldsList.add(new Object[] {"roleCreate","Role for create",DATATYPE_LIST,DATATYPE_STRING,rolesList});
        fieldsList.add(new Object[] {"roleRead","Role for read",DATATYPE_LIST,DATATYPE_STRING,rolesList});
        fieldsList.add(new Object[] {"roleUpdate","Role for update",DATATYPE_LIST,DATATYPE_STRING,rolesList});
        fieldsList.add(new Object[] {"roleDelete","Role for delete",DATATYPE_LIST,DATATYPE_STRING,rolesList});
        fieldsList.add(new Object[] {"creatable","Is creatable",DATATYPE_BOOLEAN,DATATYPE_BOOLEAN,"true"});
        fieldsList.add(new Object[] {"editable","Is Editable",DATATYPE_BOOLEAN,DATATYPE_BOOLEAN,"true"});
        fieldsList.add(new Object[] {"requireAuth","Require auth",DATATYPE_BOOLEAN,DATATYPE_BOOLEAN,"true"});
        
        
        return fieldsList;
        
    }

    
    public  Map<Integer,Object> getModel(int object_type) {
    
            Map<Integer,Object> fieldsMap = new LinkedHashMap<>();
           
            int index=0;
            

                for (Object[] fieldArray : getFieldsList()) {

                    Map<String,Object> field = new LinkedHashMap<>();
                    field.put("name", fieldArray[0]);
                    field.put("displayname", fieldArray[1]);

                    switch ((int)fieldArray[2]) {
                        case DATATYPE_HIDDEN:
                            field.put("datatype", "hidden");
                            field.put("values",fieldArray[4]);
                            break;                        
                        case DATATYPE_BOOLEAN:
                            field.put("datatype", "boolean");
                            field.put("values",fieldArray[4]);
                            break;
                        case DATATYPE_STRING:
                            field.put("datatype", "string");
                            field.put("values",fieldArray[4]);
                            break;
                        case DATATYPE_LONG:
                            field.put("datatype", "long");
                            field.put("values",fieldArray[4]);
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
    
    public void getCreateModel () {
        
        Map<Integer, Object> fieldsMap = getModel(OBJECT_TYPE_SIMPLE);
        
        String catalog = (String)getParameter("catalog");
        
        addToDataModel("catalog", catalog);        
        
        addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','&catalog=" + catalog +"')");
        
        addToDataModel("submit_to_module", getModuleName());
        addToDataModel("submit_to_action", "create");  

        addToDataModel("fields", fieldsMap);
        
        try {
            renderData("/development/dev_Createobject");
        } catch (IOException ex) {
            Logger.getLogger(NCodeObjects.class.getName()).log(Level.SEVERE, null, ex);
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
        
        String objectName = (String)getParameter("name");
        String catalog = (String)getParameter("catalog"); 
        
        int resultCode = createInSystem(objectName,catalog,null,getRequestObject().getParams());

        switch (resultCode) {
            case ERROR_OBJECT_NAME_FORMAT:
                addToDataModel("result", "error");
                addToDataModel("errorCode", "1001");
                addToDataModel("message", "Wrong format of object name !");
                renderData();
                return;
            case ERROR_OBJECT_ALREADY_EXIST:
                addToDataModel("result", "error");
                addToDataModel("errorCode", "1002");
                addToDataModel("message", "Object already exists !");
                renderData();
                return;
            case SUCCESS:
                addToDataModel("message","Object '" + objectName + "' created !");
                addToDataModel("result", "success");
                renderData();
                break;
            default:
                break;
        }
        
        
    }    
    
    private void delete() {
        
        String name = (String)getParameter("name");
        
        CrudObject nCrudObject = new ObjectLoader().get(name);
        
        if (nCrudObject == null) {
                addToDataModel("message","Object '" + nCrudObject.getName() + "' not exists !");
                addToDataModel("result", "error");
                renderData();
            
        } else {

            File f = new File(nCrudObject.getFilepath());

            if (f.delete()) {

                    new ObjectLoader().unload(name);

                    addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','')");             

                    addToDataModel("message","Object '" + nCrudObject.getName() + "' deleted !");
                    addToDataModel("result", "success");            

                    renderData();

            }
        }
        
    }
    
    
    
    private void read() {
        

                String catalog = (String)getParameterOrDefault("catalog","");
                
        
                Map<String,Object> folderData = new NFileExplorer(getGlobalEnvs(),getUsername()).exploreItemByClass(getModuleName(), catalog);
                
                folderData.forEach((key,value) -> { addToDataModel(key, value); });
                
                Format dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                //////////////// OBJECT LIST ////////////////////////
                NCodeButtons buttons = new NCodeButtons();
                
                Map<Integer,Map<String,Object>> tableRows = new LinkedHashMap<>();                   
                
                List<String> headers = new LinkedList<>();
                headers.add("Name");
                headers.add("Title");
                headers.add("Tables");
                //headers.add("Fields");
                headers.add("Last modified");
                headers.add("Actions");                
                
                int index=0;
                

                
                File[] files = new File(getGlobalEnvs().get(getModuleName() + "_path").toString() + "/" + catalog).listFiles();
                Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());                
                
                for (File file : files) {
                    
                        if (file.isDirectory()) continue;
                            //System.out.println("FILE OBJ: " + file.getName());
                            
                            String objectName_withoutextension = file.getName().split("\\.")[0];
                            
                            CrudObject nCrudObject = new ObjectLoader().get(objectName_withoutextension);

                            if (nCrudObject == null || nCrudObject.getFilepath()==null) { 
                                
                                continue;
                            }

                        Map<String,Object> columns_n = new LinkedHashMap<>();                        

                        columns_n.put(String.valueOf(columns_n.size()),String.valueOf(index));
                        columns_n.put(String.valueOf(columns_n.size()),nCrudObject.getName());
                        columns_n.put(String.valueOf(columns_n.size()),nCrudObject.getTitle());
                        if (nCrudObject.getObjectType()==CrudObject.OBJECT_TYPE_TABLE) {
                            if (!nCrudObject.getCrudTables().isEmpty()) {
                                columns_n.put(String.valueOf(columns_n.size()),nCrudObject.getDefaultCrudTable().getName());
                                //columns_n.put(String.valueOf(columns_n.size()),String.valueOf(nCrudObject.getDefaultCrudTable().getFieldList().size()));                        
                            } else {
                                columns_n.put(String.valueOf(columns_n.size()),"");
                                columns_n.put(String.valueOf(columns_n.size()),"");
                            }
                        } else {
                            columns_n.put(String.valueOf(columns_n.size()),"none");
                            columns_n.put(String.valueOf(columns_n.size()),"none");
                        }



                        Date date = new Date(nCrudObject.getTimeModified());



                            //columns.add(dateFormat.format(date));
                            columns_n.put(String.valueOf(columns_n.size()),dateFormat.format(date));


                                Map<Integer,Map<String,Object>> buttonsInTable = new LinkedHashMap<>();
                                Map<String,Object> buttonContainer = new LinkedHashMap<>();


                                Map<String,Object> button_edit = buttons.
                                                                 createButton("Edit", 
                                                                              "module=" + getModuleName() + "&action=edit&name=" + nCrudObject.getName(), 
                                                                              "orange", 
                                                                              false, 
                                                                              "fa fa-pencil", 
                                                                              "new-page");


                                Map<String,Object> button_delete = buttons.
                                                                 createButton("Delete", 
                                                                              "module=" + getModuleName() + "&action=delete&name=" + nCrudObject.getName(), 
                                                                              "red", 
                                                                              true, 
                                                                              "fa fa-trash-o", 
                                                                              "modal-xs");                        

                                buttonsInTable.put(1, button_edit);
                                buttonsInTable.put(2, button_delete);                    


                        buttonContainer.put("buttons", buttonsInTable);
                        
                    columns_n.put(String.valueOf(columns_n.size()),buttonContainer);
                    
                    tableRows.put(index, columns_n);
                    index++;
                }
                

                addToDataModel("module", getModuleName());
                addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','')");
                addToDataModel("headers", headers);
                addToDataModel("data", tableRows);
                addToDataModel("pagetitle", "Object");
                addToDataModel("pageLength", "100");
                
                try {            
                    renderData("/development/dev_Objects");
                } catch (IOException ex) {
                    Logger.getLogger(NCodeObjects.class.getName()).log(Level.SEVERE, null, ex);
                }
    }
    
    private void readEmbed() {


                Map<Integer,Map<String,Object>> tableRows = new LinkedHashMap<>();
                
                Format dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                int index=0;
                
                NCodeButtons buttons = new NCodeButtons();
                
                List<String> headers = new LinkedList<>();
                headers.add("Obj. name");
                headers.add("Obj. title");
                headers.add("Obj. tables");
                headers.add("Obj. fields");
                headers.add("Last modified");
                headers.add("Actions");
                
                ObjectLoader loader = new ObjectLoader();
                
                for (CrudObject nCrudObject : loader.getList()) {
                    
                    

                    
                    if (nCrudObject.getFilepath()==null) continue;
                    
                    Map<String,Object> columns_n = new LinkedHashMap<>();
                    
                    columns_n.put("0",String.valueOf(index));
                    columns_n.put("1",nCrudObject.getName());
                    columns_n.put("2",nCrudObject.getTitle());
                    if (nCrudObject.getObjectType()==CrudObject.OBJECT_TYPE_TABLE) {
                        if (!nCrudObject.getCrudTables().isEmpty()) {
                            columns_n.put("3",nCrudObject.getDefaultCrudTable().getName());
                            columns_n.put("4",String.valueOf(nCrudObject.getDefaultCrudTable().getFieldList().size()));                        
                        } else {
                            columns_n.put("3","");
                            columns_n.put("4","");
                        }
                    } else {
                        columns_n.put("3","none");
                        columns_n.put("4","none");
                    }


                    
                    Date date = new Date(nCrudObject.getTimeModified());

       
                    
                    //columns.add(dateFormat.format(date));
                    columns_n.put("5",dateFormat.format(date));
                    
                    
                        Map<Integer,Map<String,Object>> buttonsInTable = new LinkedHashMap<>();
                        Map<String,Object> buttonContainer = new LinkedHashMap<>();
                        
                        
                        Map<String,Object> button_show = buttons.
                                                         createButton("Show", 
                                                                      "module=" + getModuleName() + "&action=edit&name=" + nCrudObject.getName(), 
                                                                      "orange", 
                                                                      false, 
                                                                      "fa fa-pencil", 
                                                                      "new-page");
                       


                        buttonsInTable.put(1, button_show);
                    
                        buttonContainer.put("buttons", buttonsInTable);
                        
                    columns_n.put("6",buttonContainer);
                    
                    tableRows.put(index, columns_n);
                    index++;
                }
                

                addToDataModel("module", getModuleName());                                
                addToDataModel("doafter","loadContainerQuery('" + getModuleName() + "','read','#netondocontentbody','')");
                addToDataModel("headers", headers);
                addToDataModel("data", tableRows);
                addToDataModel("title", "Object");
                addToDataModel("pageLength", "100");
                
                try {            
                    renderData("/development/dev_Objects");
                } catch (IOException ex) {
                    Logger.getLogger(NCodeObjects.class.getName()).log(Level.SEVERE, null, ex);
                }
    }    

    
    public void edit() {
        
        String name = (String)getParameter("name");
        
        
        String filepath = new ObjectLoader().get(name).getFilepath();
        
        addToDataModel("filetype", "json");
        
        String path = (String)getGlobalEnvs().get("objects_path");
        
        addToDataModel("extension","object");
        addToDataModel("name",name);
        addToDataModel("filetitle",name.replaceFirst(path + "/", ""));         

        addToDataModel("editmode",getModuleName());
        addToDataModel("submit_to_module", getModuleName());
        addToDataModel("submit_to_action", "save");
        
        addToDataModel("content", getContent(filepath));
        
        try {
            renderData("/development/editor/dev_Codeeditor");
        } catch (IOException ex) {
            Logger.getLogger(NCodeObjects.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    } 
    
    public void save() {
        
                String objcode = (String)getParameter("code");
                String objectName = (String)getParameter("name");
                
                        int returnCode = saveInSystem(objectName, objcode);

                        switch (returnCode) {
                            case ERROR_OBJECT_NAME_FORMAT:
                                addToDataModel("result", "error");
                                addToDataModel("errorCode", "1001");
                                addToDataModel("message", "Wrong format of object name !");
                                renderData();
                                break;
                            case ERROR_OBJECT_NAME_CHANGED:
                                addToDataModel("message","Object name can not be changed !");
                                addToDataModel("result", "error");
                                renderData();
                                break;
                            case SUCCESS:
                                addToDataModel("message","Object '" + objectName + "' saved !");
                                addToDataModel("result", "success");
                                renderData();
                                break;
                            case ERROR_OBJECT_NOT_FOUND:
                                addToDataModel("message","Object file not found !");
                                addToDataModel("result", "error");
                                renderData();
                                break;
                            case ERROR_OBJECT_PARSE:
                                addToDataModel("message","Object is invalid and not saved !");
                                addToDataModel("result", "error");
                                renderData();
                                break;
                            case ERROR_OBJECT_IO:
                                addToDataModel("message","Error on saving object !");
                                addToDataModel("result", "error");
                                renderData();
                                break;
                            default:
                                break;
                        }


    }   
    
    public int saveInSystem(String objectName , String objcode) {
        
                
                byte[] decodedBytes = Base64.getDecoder().decode(objcode);
                String decodedString = new String(decodedBytes);        
               

                Pattern p = Pattern.compile("[^a-z0-9 _-]", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(objectName);
                boolean isPermittedSymbolsFoundInObjectName = m.find();

                if (isPermittedSymbolsFoundInObjectName) {
                    return ERROR_OBJECT_NAME_FORMAT;
                }                  
                
                JSONParser parser = new JSONParser();
                
                ObjectLoader loader = new ObjectLoader();
                
                    try {
                        
                        JSONObject jSONObject = (JSONObject) parser.parse(decodedString);
                        
                        String newObjectName = (String)jSONObject.get("name");
                        
                        File newfile;
                        
                        if (newObjectName.equals(objectName)) {
                            newfile = new File(loader.get(objectName).getFilepath() );
                        } else {  
                                return ERROR_OBJECT_NAME_CHANGED;
                        }
                                
                        if (newfile.exists()) {
                            
                            try (FileWriter fw = new FileWriter(newfile)) {
                                fw.write(decodedString);
                                fw.flush();
                            }
                            
                            loader.unload(objectName);
                            loader.load(newfile);
                            
                            return SUCCESS;
                        } else {
                            return ERROR_OBJECT_NOT_FOUND;
                        }

                        
                    } catch (ParseException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);

                        return ERROR_OBJECT_PARSE;

                    } catch (IOException ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                        return ERROR_OBJECT_IO;
                    }

        
    } 
    
    public int createInSystem(String objectName,String path,String objType,Map<String,Object> params) {
        
        Pattern p = Pattern.compile("[^a-z0-9 _-]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(objectName);
        boolean isPermittedSymbolsFoundInObjectName = m.find();

        if (isPermittedSymbolsFoundInObjectName) {
            return ERROR_OBJECT_NAME_FORMAT;
        }
        
        if (!path.equals("")) path = path + "/";
        
        String objectFilePath = (String)getGlobalEnvs().get("objects_path") + "/" + path + objectName + ".json";
        
        File objectFile = new File(objectFilePath);
        
        if (objectFile.exists()) {
            return ERROR_OBJECT_ALREADY_EXIST;            
        } else {
            try {
                if (objectFile.createNewFile()) { 
                    
                    try ( FileWriter fw = new FileWriter(objectFile)) {
                        Map dataMapForJson = new LinkedHashMap();
                        dataMapForJson.put("objectType","table");
                        //dataMapForJson.put("operationType","create,read,update,delete");
                        dataMapForJson.put("objectType","table");
                       

                        Map<Integer, Object> fieldsMap = getModel(OBJECT_TYPE_SIMPLE);

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
                                    
                                    dataMapForJson.put((String)fieldData.get("name"), nObject);
                                    String fieldName = (String)fieldData.get("name");
                                    if (fieldName.equals("tables")) {
                                        
                                            if (paramValue.length()==0) break;
                                            
                                            Map<String,Map<String,Object>> columnData = new DbMetaData(new DataConnector(getGlobalEnvs(),getUsername())).getColumnList(paramValue);

                                            columnData.forEach((column,options) -> {
                                                options.remove("dataType");
                                                options.remove("length");
                                                
                                                String column_a = column.replaceAll("_", " ").replaceAll("-", " ");
                                                
                                                column_a = column_a.substring(0, 1).toUpperCase() + column_a.substring(1);
                                                options.put("displayName", column_a);
                                            });

                                            JSONObject nObject_of_table = new JSONObject();

                                            Map.Entry<String,Map<String,Object>> entry = columnData.entrySet().iterator().next();
                                            String key = entry.getKey();
                                            JSONObject idJsonObject = new JSONObject();
                                            idJsonObject.put("id", key);

                                            columnData.remove(key);

                                            nObject_of_table.put("id-field", idJsonObject);
                                            nObject_of_table.put("fields", columnData);
                                            nObject.put(paramValue, nObject_of_table);
                                            
                                    }                                    
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
                                case DATATYPE_DOUBLE:
                                    dataMapForJson.put((String)fieldData.get("name"), Double.parseDouble(paramValue));
                                    break;
                                case DATATYPE_TEXT:
                                    dataMapForJson.put((String)fieldData.get("name"), paramValue);
                                    break;
                                    
                                default:
                                    break;
                            }


                        }
                        
                        dataMapForJson.put("templateForCreate","/defaultTemplates/createObjectData");
                        dataMapForJson.put("templateForRead","/defaultTemplates/readObjectData");
                        dataMapForJson.put("templateForListRead","/defaultTemplates/readListObjectData");
                        dataMapForJson.put("templateForUpdate","/defaultTemplates/updateObjectData");
                        dataMapForJson.put("templateForDelete","/defaultTemplates/deleteObjectData");
                        dataMapForJson.put("templateForFilters","/defaultTemplates/readObjectDataFilters"); 

                        
                        Map<String,Object> metaData = new LinkedHashMap<>();

                        
                        metaData.put("pagetitle", params.get("title").toString());
                        metaData.put("pageLength","100");
                        metaData.put("editButtonInTable",true);
                        

                        
                        
                        dataMapForJson.put("metadata", metaData);


                        var dataUtils = new DataUtils();

                        fw.write(new DataUtils().beautyfyJson(dataUtils.mapToJsonString(dataMapForJson)));
                        fw.flush();
                        
                        new ObjectLoader().load(objectFile);
                    
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
