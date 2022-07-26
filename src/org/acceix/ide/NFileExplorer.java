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

package org.acceix.ide;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.acceix.logger.NLog;
import org.acceix.logger.NLogBlock;
import org.acceix.logger.NLogger;


public class NFileExplorer {
    
    
    private Map<String,Object> ENVS;
    
    private String username;
    
    public static int CATALOG_EXISTS = 1;
    public static int CATALOG_CREATED = 2;
    public static int CATALOG_NOT_CREATED = 3;
    
    public static int CATALOG_NOT_EXISTS = 1;
    public static int CATALOG_NOT_DELETED = 2;
    public static int CATALOG_DELETED = 3;
    public static int CATALOG_NOT_EMPTY = 4;
    
    public Map<String,Object> getGlobalEnvs() {
        return ENVS;
    }
    
    public void setGlobalEnvs(Map<String,Object> envs) {
        this.ENVS = envs;
    }

    public NFileExplorer(Map<String, Object> ENVS,String username) {
        this.ENVS = ENVS;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
    
    
    
    public int createCatalog(String itemClass,String catalog) {

                 if (catalog.startsWith("/"))
                        catalog = catalog.replaceFirst("/", "");

                File fileToCreate = new File(getGlobalEnvs().get(itemClass + "_path").toString() + "/" + catalog);
                
                
                if (fileToCreate.exists()) {
                    return CATALOG_EXISTS;
                } else {
                    if (fileToCreate.mkdir()) {
                        return CATALOG_CREATED;
                    } else {
                        return CATALOG_NOT_CREATED;
                    }
                }
                                        
    }
    
    public int deleteCatalog(String itemClass,String catalog) {

                if (catalog.startsWith("/"))
                    catalog = catalog.replaceFirst("/", "");

                File fileToDelete = new File(getGlobalEnvs().get(itemClass + "_path").toString() + "/" + catalog);

                
                if (fileToDelete.listFiles().length > 0) {
                    return CATALOG_NOT_EMPTY;
                }
                
                if (fileToDelete.exists()) {
                    fileToDelete.delete();
                    return CATALOG_DELETED;
                } else {
                    return CATALOG_NOT_EXISTS;
                }
                                        
    }    
    
    
    public Map<String,Object> exploreItemByClass(String item_class,String catalog) {
        
        
                String globalPath = getGlobalEnvs().get("home_path").toString();
                
                String itemPath = getGlobalEnvs().get(item_class + "_path").toString();

                Map<Integer,Map<String,Object>> folder_tableRows = new LinkedHashMap<>();
                

                
                int index=0;
                
                List<String> folder_Tableheaders = new LinkedList<>();
                
                if (catalog.startsWith("/") || catalog.startsWith(".") || catalog.contains("..")) catalog = ""; 
                    Map<String,Object> folders_column_current_path = new LinkedHashMap<>();                
                    folders_column_current_path.put(String.valueOf(folders_column_current_path.size()),
                                                                   itemPath.replaceFirst(globalPath, "").replaceAll("/", " / ") + " / " + catalog.replaceAll("/", " / ") + " ");


                 folder_tableRows.put(index, folders_column_current_path);
                 index++;
 
                if (!catalog.equals("")) {
                    
                    String go_to_parent = "<a href=\"#\" onClick=\"loadContainerQuery('" + item_class + "','read','#netondocontentbody','" 
                                        + "catalog=" 
                                        +  new File(itemPath + "/" + catalog).getParent().replaceFirst(itemPath, "").replaceFirst("/", "")
                                        + "');\">"
                                        + "<i class=\"fa fa-folder-open ml-3\"></i><span>..</span></a>";

                         Map<String,Object> folders_column_go_to_parent = new LinkedHashMap<>();                
                         folders_column_go_to_parent.put(String.valueOf(folders_column_go_to_parent.size()),go_to_parent);
                         folder_tableRows.put(index, folders_column_go_to_parent);    
                         index++;
                         
                }



                 String create_new_folder = "<a href=\"#\" onClick=\"createFolder('" + catalog + "');\"><i class=\"fa fa-plus-square ml-0\"></i><span class=\"ml-1 primary\">Create new catalog</span></a>";


                 folder_Tableheaders.add(create_new_folder);
                    
                    var objectsDir = new File(itemPath + "/" + catalog);

                    if (!objectsDir.exists() || !objectsDir.isDirectory()) {
                        NLogger.logger(NLogBlock.WEB_CRUD,NLog.ERROR,"NFileExplorer","exploreItemByClass",getUsername(),"No objects folder on path: " + itemPath);
                    }                 
                
                File[] files = objectsDir.listFiles();
                Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                        
                for (File objfile : files) {
                    
                        if(objfile.isDirectory()) {
                            
                                File[] filesInside = objfile.listFiles();
                            
                                Map<String,Object> folders_columns_n = new LinkedHashMap<>();
                                
                                String deleteLink = "<div style=\"display: table-cell; width: 0%;\"><a href=\"#\" onClick=\"deleteFolder('" + objfile.getAbsolutePath().replaceFirst(itemPath + "/", "") + "');\">"
                                                    + "<i class=\"fa fa-trash-o warning  ml-3\"></i></a></div>";
                                
                                
                                String folderLink = "<div style=\"display: table-cell\"><a href=\"#\" onClick=\"loadContainerQuery('" + item_class + "','read','#netondocontentbody','"
                                                    + "catalog=" + objfile.getAbsolutePath().replaceFirst(itemPath + "/", "") + "');\">"
                                                    + "<i class=\"fa fa-folder ml-3\"></i><span class=\"ml-1\">" + objfile.getName()  + " (" + filesInside.length + ")"+ "</span></a></div>";
                                
                                
                                if (filesInside.length == 0) {
                                    folderLink = folderLink.replaceFirst("fa fa-folder ml-3", "fa fa-folder warning ml-3");
                                    String folderColumn = "<div style=\"display: table;\"><div style=\"display: table-row\">" + folderLink + deleteLink + "</div>";                                
                                    folders_columns_n.put(String.valueOf(folders_columns_n.size()),folderColumn);
                                    
                                } else {
                                    deleteLink = "<div style=\"display: table-cell\"></div>";
                                    String folderColumn = "<div style=\"display: table;\"><div style=\"display: table-row\">" + folderLink + deleteLink + "</div></div>";                                
                                    folders_columns_n.put(String.valueOf(folders_columns_n.size()),folderColumn);
                                                                       
                                }                         


                                folder_tableRows.put(index, folders_columns_n);
                                index++;
                                
                        }                    
                        
                }
                
                Map<String,Object> retValue = new LinkedHashMap<>();
                
                
                retValue.put("folder_Tableheaders", folder_Tableheaders);
                retValue.put("folder_tableRows", folder_tableRows); 
                retValue.put("current_catalog", catalog);
                
                return retValue;

                        
    }
    
}
