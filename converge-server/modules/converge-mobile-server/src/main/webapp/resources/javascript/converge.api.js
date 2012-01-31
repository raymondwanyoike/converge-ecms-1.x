/* 
 *  Copyright (C) 2011 Interactive Media Management. All Rights Reserved.
 * 
 *  NOTICE:  All information contained herein is, and remains the property of 
 *  INTERACTIVE MEDIA MANAGEMENT and its suppliers, if any.  The intellectual 
 *  and technical concepts contained herein are proprietary to INTERACTIVE MEDIA
 *  MANAGEMENT and its suppliers and may be covered by Danish and Foreign 
 *  Patents, patents in process, and are protected by trade secret or copyright 
 *  law. Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained from 
 *  INTERACTIVE MEDIA MANAGEMENT.
 */

function cnvms_login(phone, password, fnSuccess, fnError) {
    $.ajax({
        url: 'services/subscriber',
        type: 'GET',
        async: false,
        data: '',
        headers: {
            'phone':phone, 
            'password':password
        },
        success: fnSuccess,
        error: fnError
    });
}
            
function cnvms_register(name, phone, gender, dob, password, fnSuccess, fnFailure) {
    // Construct JSON message containing registration details
    var json = '{"name":"' + name + '", ' +
    '"phone":"' + phone + '", '+
    '"gender":"' + gender + '", ' +
    '"dob":"' + dob + '", ' + 
    '"password":"' + password + '"}';
    
    $.ajax({
        url: 'services/subscriber/register',
        contentType: "application/json; charset=utf-8", 
        type: 'POST',
        dataType: 'json',
        data: json,
        success: fnSuccess,
        error: fnFailure
    });
    return false;
}

function cnvms_categories(phone, password, outletId, outletKey, fnSuccess, fnError) {    
    $.ajax({
        url: 'services/subscriber/sections',
        type: 'GET',
        dataType: 'json',
        async: false,
        data: '',
        headers: {
            'phone':phone, 
            'password':password,
            'outlet':outletId,
            'key':outletKey
        },
        success: fnSuccess,
        error: fnError
    });
}


function cnvms_fetchnews(phone, password, fnSuccess, fnError) {
    $.ajax({
        url: 'services/subscriber/news',
        type: 'GET',
        async: false,
        dataType: 'json',
        data: '',
        headers: {
            'phone':phone, 
            'password':password
        },
        success: fnSuccess,
        error: fnError
    });
}

function cnvms_read(phone, password, read) {
    $.ajax({
        url: 'services/subscriber/readNews',
        type: 'POST',
        contentType: "application/json; charset=utf-8", 
        dataType: 'json',
        data: '[' + read + ']',
        headers: {
            'phone':phone, 
            'password':password
        }
    });
}

function cncms_subscribe(phone, password, categories) {
    var subscriptions = "[";
    var first = true;
    for (i=0;i<categories.length;i++) {
        if (!first) {
            subscriptions += ",";
        } else {
            first = false;
        }
        subscriptions += categories[i];
    }
    subscriptions += "]";
    
    $.ajax({
        url: 'services/subscriber/subscribe',
        type: 'POST',
        contentType: "application/json; charset=utf-8", 
        dataType: 'json',
        data: subscriptions,
        async: false,
        headers: {
            'phone':phone, 
            'password':password
        },
        error: function(){
            alert('Could not save subscriptions. Please try later');
        }
    });
    
}

