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

/*
 * This script is the controller between index.html and converge.api.js, dealing
 * with any interaction on the index.html page
 */


/** 
 *  Global variable containing information about the logged-in subscriber 
 *  and his/her preferences. 
 */
var mobile_subscriber;

/**
 * Global variable containing the news downloaded for the logged-in subscriber.
 */
var mobile_news;

// Initialises jQuery scripts
$(document).ready(function() {
	// binds form submission and fields to the validation engine
    jQuery("#registrationForm").validationEngine();
    jQuery("#loginForm").validationEngine();
        
    clearValidationErrors();
    hookLogin();
    hookRegister();
    hookSavePreferences();
    hookRefresh();
});

String.prototype.trunc = function(n){
    return this.substr(0,n-1)+(this.length>n?'...':'');
};

function populatePreferences(categories) {
    for (i=0; i<categories.section.length; i++) {
        var category = categories.section[i];
        //Display only non-special categories for the user to choose from
        if (category.special=="false") {
            $('#prefCats').append('<input type="checkbox" name="cbCat" id="cbCat' + category.id + '" value="' + category.id + '" /><label for="cbCat' + category.id + '">' + category.title + '</label>');
        }
        else{
            //TODO: Handle the scenario of no optional categories to choose from. Follow up
            $('#cats').append('<li><a href="#cat' + mobile_subscriber.subscriptions[i].id + '">' + mobile_subscriber.subscriptions[i].title + '</a><span class="ui-li-count">' + category_count + '</span></li>>');
            
        }
      
	}

    // Check current preferences
    if (mobile_subscriber.subscriptions != undefined) {
        for (i=0; i< mobile_subscriber.subscriptions.length; i++) {
            $('#cbCat' + mobile_subscriber.subscriptions[i].id).attr('checked', true);
        }
    }
}

function refreshCategories() {
    if (mobile_subscriber.subscriptions != undefined) {
        
        var category_ids = new Array();
            
        $('#lstSubscriptions').html("");
        for (i=0; i<mobile_subscriber.subscriptions.length; i++) {
            var news_item_category = mobile_subscriber.subscriptions[i];
            
            category_ids.push(news_item_category.id);
            
            var category_count = 0;
            for (j=0; j<mobile_news.newsItem.length;j++) {
                var tmpNewsItem = mobile_news.newsItem[j];
                if (tmpNewsItem.section.id == news_item_category.id) {
                    category_count++;
                }
            }
                     
            $('#lstSubscriptions').append('<li><a href="#cat' + mobile_subscriber.subscriptions[i].id + '">' + mobile_subscriber.subscriptions[i].title + '</a><span class="ui-li-count">' + category_count + '</span></li>');

            // Removing existing category page if it exists
            $('#cat' + mobile_subscriber.subscriptions[i].id).remove();

            $('body').append(
                '<div data-role="page" id="cat' + mobile_subscriber.subscriptions[i].id + '" data-url="cat'+mobile_subscriber.subscriptions[i].id+'">' +
                '    <div data-role="header" data-position="fixed" data-theme="a" >' +
                '        <a href="#cats" data-icon="arrow-l" data-back="true" data-theme="a" >Back</a>' +
                '        <h1>' + mobile_subscriber.subscriptions[i].title + '</h1>' +
                '    </div>' +
                '    <div data-role="content" data-theme="c">' +
                '        <ul id="lstCat' + mobile_subscriber.subscriptions[i].id + '" data-role="listview" data-url="lstCat' + mobile_subscriber.subscriptions[i].id + '">' +
                '        </ul>' +
                '    </div>' +
                '    <div data-role="footer" data-theme="a" data-position="fixed" id="newsCatAd'+news_item_category.id+'">' +
                '       <div class="ads"> ' +
                '           <iframe id=\'ac238136'+ news_item_category.id+'\' name=\'ac238136'+ news_item_category.id+'\' src=\'http://www.the-star.co.ke/ads/www/delivery/afr.php?zoneid=1&amp;cb=INSERT_RANDOM_NUMBER_HERE\' frameborder=\'0\' scrolling=\'no\' width=\'234\' height=\'60\' allowtransparency=\'true\'><a href=\'http://www.the-star.co.ke/ads/www/delivery/ck.php?n=a311071e&amp;cb=INSERT_RANDOM_NUMBER_HERE\' target=\'_blank\'><img src=\'http://www.the-star.co.ke/ads/www/delivery/avw.php?zoneid=1&amp;cb=INSERT_RANDOM_NUMBER_HERE&amp;n=a311071e\' border=\'0\' alt=\'\' /></a></iframe>' +
                '       </div>' + 
                '   </div>' +

                '</div>');
           
            for (j=0; j<mobile_news.newsItem.length;j++) {
                
                var newsItem = mobile_news.newsItem[j];
                if (newsItem.section.id == news_item_category.id) {
                    var story = mobile_news.newsItem[j].story.replace(/(<([^>]+)>)/ig,"");
                
                    // Story List
                    $('#lstCat' + mobile_subscriber.subscriptions[i].id).append(
                        '<li><a id="lnkNewsItem' + newsItem.id  + '" href="#newsItem' +mobile_news.newsItem[j].id  + '">' +
                        '<img src="' + newsItem.thumbUrl + '" class="ui-li-thumb" />' +
                        '<h3>' +mobile_news.newsItem[j].headline  + '</h3>' +
                        '<p>' + story.trunc(150)  + '</p></a></li>');
                    
                    // Story Page
                    $('body').append(
                        '<div data-role="page" id="newsItem' + newsItem.id + '" data-url="newsItem'+newsItem.id+'">' +
                        '    <div data-role="header" data-position="fixed" >' +
                        '        <a href="#cat' + mobile_subscriber.subscriptions[i].id + '" data-icon="arrow-l" data-back="true" data-theme="a" >Back</a>' +
                        '        <h1>' + newsItem.headline + '</h1>' +
                        '    </div>' +
                        '    <div data-role="content" data-theme="d">' +
                        '       <p class="dateline">' + newsItem.dateline + ' - ' + news_item_category.title + '</p>' +
                        '       <p><img src="' + newsItem.imgUrl + '" style="float: left; margin-right: 7px; margin-bottom: 7px;" />' + newsItem.story + '</p>' +
                        '    </div>' +
                        '    <div data-role="footer" data-theme="a" data-position="fixed" id="newsItemAd'+newsItem.id+'">' +
                        '       <div class="ads"> ' +
                        '           <iframe id=\'ac238136'+ newsItem.id+'\' name=\'ac238136'+ newsItem.id+'\' src=\'http://www.the-star.co.ke/ads/www/delivery/afr.php?zoneid=1&amp;cb=INSERT_RANDOM_NUMBER_HERE\' frameborder=\'0\' scrolling=\'no\' width=\'234\' height=\'60\' allowtransparency=\'true\'><a href=\'http://www.the-star.co.ke/ads/www/delivery/ck.php?n=a311071e&amp;cb=INSERT_RANDOM_NUMBER_HERE\' target=\'_blank\'><img src=\'http://www.the-star.co.ke/ads/www/delivery/avw.php?zoneid=1&amp;cb=INSERT_RANDOM_NUMBER_HERE&amp;n=a311071e\' border=\'0\' alt=\'\' /></a></iframe>' +
                        '       </div>' +
                        '   </div>' +
                        '</div>');
           
                    
                    $('#lnkNewsItem' + newsItem.id).click(function(event) {
                        cnvms_read($('#phone').val(), $('#password').val(), newsItem.id);
                    });
                }
            }
        }
	   
//	   $('#lstSubscriptions').append(
//            '<li data-theme="e">' +
//             '#lstSpecial'
//           '<img src="images/maina.png" class="ui-li-thumb" />'
//            '<h3>Maina\'s List</h3>' +
//           '<p>Coming soon!</p></li>'
//        );
            
//        $('#lstSubscriptions').append(
//            '<li data-theme="e">' +
//            '<img src="images/caroline.png" class="ui-li-thumb" />' +
//            '<h3>Caroline\'s Jobs</h3>' +
//            '<p>Coming soon!</p></li>');

        
       //  Re-render category pages
        for (i=0; i<category_ids.length; i++) {
            $('#cat' + mobile_subscriber.subscriptions[i].id).page();
        }
        
        $('#cats').page();
    }    
}

function checkPasswords(pass1, pass2) {
    if (pass1.value != pass2.value) {
        pass1.setCustomValidity("Your passwords do not match. Please recheck that your new password is entered identically in the two fields.");
    } else {
        pass1.setCustomValidity("");
    }
}

function fetchSubscriptions(phone, password) {
    cnvms_login(phone, password, function(data) {
        mobile_subscriber = data;
            
        cnvms_fetchnews(phone, password, function(newsData) {
            mobile_news = newsData
        }, 
	   function() {
            alert('Could not fetch news')
        });
    },
    function() {
	    alert("Invalid phone number or password");
    });
}

function hookLogin() {
    $('#btnLogin').click(function(event) {
        
        var phone = $('#phone').val();
        var password = $('#password').val();
        
        fetchSubscriptions(phone, password);
        
        refreshCategories();

        cnvms_categories($('#phone').val(), $('#password').val(), 1, 'abcd1234',
            function(data) {
                populatePreferences(data);
            },
            function() {
                alert('Preferences could not be fetched');
            }
            );
                    
        $.mobile.changePage($("#cats"));            
    });
    
   	
}

function clearValidationErrors() {
    $('#registrationPage').click(function(event) {
        jQuery("#loginForm").validationEngine('hide');
        $.mobile.changePage($("#register"));   
        
    });
}

function hookRegister() {
    $('#btnRegister').click(function(event) {
        
        cnvms_register($('#registrationName').val(), $('#registrationPhone').val(), $('input[name=gender]:checked').val(), $('#registrationDob').val(), $('#registrationPassword').val(), 
            function() {
                document.location = '#login';
                alert("Thanks for your registration, you may now proceed to log-in");
                jQuery("#registrationForm").validationEngine('hide');
            },
            function() {
                alert($('#registrationPhone').val() + " is already registered");
            }
            );    
    });  
}

function hookSavePreferences() {
    $('#btnSavePreferences').click(function(event) {
        var save_categories = new Array();
        $("input:checkbox:checked").each(function(i){
            save_categories.push(this.value);
        });
        
        var phone = $('#phone').val();
        var password = $('#password').val();
        
        cncms_subscribe(phone, password, save_categories);
        fetchSubscriptions(phone, password);
        
        refreshCategories();        
        $('#lstSubscriptions').listview('refresh');
    });
}

function hookRefresh() {
    $('#btnRefresh').click(function(event) {
        // Download
        var phone = $('#phone').val();
        var password = $('#password').val();
        fetchSubscriptions(phone, password);
        
        refreshCategories();        
        $('#lstSubscriptions').listview('refresh');
    });
}
