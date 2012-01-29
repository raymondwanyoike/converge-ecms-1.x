/*
 * Copyright (C) 2011 ferrycode
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ferrycode.tahrir.jsf.beans;

import dk.i2m.converge.jsf.beans.*;
import java.util.Locale;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

/**
 *
 * @author ferrycode
 */
public class UILocale {
    
    
    
    private Locale locale;
    private String lang;
    private String htmldir;
    
    private Locale currentViewLocale;
    private String currentViewLang;

    public String getCurrentViewLang() {
        currentViewLang = FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage();
        return currentViewLang;
    }

    public Locale getCurrentViewLocale() {
        currentViewLocale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        return currentViewLocale;
    }

    
    //UserSession userSession;
    
    
    public UILocale(){
    super();
    
    }
    
    /**
     * Creates a new instance of {@link UserSession}.
     */
    public UILocale(Locale locale) {
        this.locale = locale;
        this.lang = locale.getLanguage();

        if(lang.equals("ar")||lang.equals("he")){  htmldir = "rtl";
        }else {htmldir="ltr";}
    
    }
     
     

  public void setLocale(Locale locale) {
    this.locale = locale;
    this.lang = locale.getLanguage();
    
    if(lang.equals("ar")||lang.equals("he")){  htmldir = "rtl";
    }else {htmldir="ltr";}
  }

  public Locale getLocale() {
    return locale;
  }

  public String changeLanguage() {
    return "changed";
  }
  
  // get current locale used by converge
  
  public Locale getCurrentLocale(){
        return FacesContext.getCurrentInstance().getViewRoot().getLocale();   
       
    }
  
   public String getCurrentLang(){
        return getCurrentLocale().getLanguage();   
       
    }
   
 
    // get default system locle
    private Locale getDefaultSystemLocale(){
        return Locale.getDefault();     
    
    }
    
    private String getDefaultSystemLang(){
        return getDefaultSystemLocale().getLanguage();     
    
    }
   
   /**
     * Author: ferrycode
     * Switch UI Locale.
     *
     * @param String locale
     *          
     * @return void
     */
    public void switchUILocale(){
        
        FacesContext.getCurrentInstance().getViewRoot().setLocale(locale);   
    
    }
    
    public void chooseLocaleFromLink(ActionEvent event) {  
          
       
            //String sLocale = (String)event.getComponent().getAttributes().get("value");  
            
            FacesContext ctx = FacesContext.getCurrentInstance();  
            String sLang = ctx.getExternalContext().getRequestParameterMap().get("lang").toString();
            
            //ctx.getViewRoot().setLocale(new Locale(sLocale));
            setLocale(new Locale(sLang));
            switchUILocale();
            
        }
    
    public void chooseLocaleFromList(ValueChangeEvent event) {          
              //TODO validate locale string  
              setLocale(new Locale(event.getNewValue().toString()));
              switchUILocale();
            
        }
    

    /**
     * @return the lang
     */
    public String getLang() {
        return lang;
    }

    

    /**
     * @return the htmldir
     */
    public String getHtmldir() {
        return htmldir;
    }    
     

    
}
