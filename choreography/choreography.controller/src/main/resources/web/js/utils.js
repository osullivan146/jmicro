var jmicro = jmicro || {};

jmicro.goTo = function(url) {
    location.href = url;
}

String.prototype.startWith=function(str){
    var reg=new RegExp("^"+str);
    return reg.test(this);
}

String.prototype.endWith=function(str){
    var reg=new RegExp(str+"$");
    return reg.test(this);
}

Date.prototype.format = function(fmt) {
    var o = {
        "M+" : this.getMonth()+1,                 //月份
        "d+" : this.getDate(),                    //日
        "h+" : this.getHours(),                   //小时
        "m+" : this.getMinutes(),                 //分
        "s+" : this.getSeconds(),                 //秒
        "q+" : Math.floor((this.getMonth()+3)/3), //季度
        "S"  : this.getMilliseconds()             //毫秒
    };
    if(/(y+)/.test(fmt)) {
        fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));
    }
    for(var k in o) {
        if(new RegExp("("+ k +")").test(fmt)){
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
        }
    }
    return fmt;
}

//for safari
if(typeof Array.prototype.fill == 'undefined') {
    Array.prototype.fill = function(e) {
     for(var i = 0; i < this.length; i++) {
         this[i] = e;
     }
    }
}

jmicro.http = {

    getQueryParam : function(name) {
        if(location.href.indexOf("?")==-1 || location.href.indexOf(name+'=')==-1){
            return '';
        }
        var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
        var r = window.location.search.substr(1).match(reg);
        if (r != null)
            return decodeURI(r[2]);   //对参数进行decodeURI解码
        return null;
    },

    get: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'GET', errCb, fullpath)
    },

    post: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'POST', errCb, fullpath)
    },

    put: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'PUT', errCb, fullpath)
    },

    ajax: function (path, params, sucCb, async, method, errCb, fullpath) {
        if (!sucCb) {
            throw "Callback method cannot be null";
        }

        if (!method) {
            throw "request method cannot be null";
        }

        if (!path) {
            path = "/";
        }

        this._doAjax(path, params, sucCb, async, method, errCb, fullpath);
    }

    ,_doAjax: function(path, params, sucCb, async, method, errCb, fullpath) {
        /* if(!jmicro.urls.checkNetwork()) {
         throw '网络连接不可用';
         }*/
        if(fullpath) {
            path = fullpath;
        } else if (!(path.startWith('http://') &&  !path.startWith('https://'))){
            path = this.getWebResourceHttpPath(path);
        }
        if(!params) {
            params = {};
        }

        $.ajax({
            data: params,
            type: method,
            async: true,
            url: path,
            jsonp: 'json',
            success: function (data, statuCode, xhr) {
                sucCb(data, statuCode, xhr);
            },
            beforeSend: function (xhr) {
                //xhr.setRequestHeader('Access-Control-Allow-Headers','*');
                /*if (jmicro.zdd.uc.isLogin()) {
                    xhr.setRequestHeader("loginKey", params.loginKey);
                }*/
            },
            error: function (err, xhr) {
                if (errCb) {
                    errCb(err, xhr);
                } else {
                    sucCb(null, err, xhr);
                }
            }
        });
    }

    ,uploadBase64Picture: function (data, ptype, cb,pos) {
        var params = {
            encode: 'base64',
            imageType: ptype,
            name: 'base64_picture_file' + jmicro.utils.getId(),
            contentType: 'image/' + ptype + ';base64',
            base64Data: data,
            pos:pos
        }
        var url = this.getWebResourceHttpPath(jmicro.http.UPLOAD_BASE64);
        jmicro.http.post(url, params, function (data1, status, xhr) {
            console.log(data1);
            if (status !== 'success') {
                cb(status, data1);
            } else {
                cb(null, data1);
            }
        }, function (err, xhr) {
            console.log(err);
            cb(err, null);
        })
    }

    ,postHelper: function(url,params,cb){
        jmicro.http.post(url,params, function(data,status,xhr){
            if(data.success) {
                cb(null,data);
            } else {
                cb(data,data);
            }
        },function(err){
            cb(err,null);
        });
    }

    //get: function (path, params, cb, errCb, fullpath)
    ,getHelper: function(url,params,cb,fullpath){
        if(!params) {
            params = {};
        }
        this.get(url,params, function(data,status,xhr){
            cb(null,data);
        },function(err){
            cb(err,null);
        },fullpath);
    }

    ,getUrl: function(url) {
        return this.getWebResourceHttpPath(url);
    },

    getWebContextPath : function() {
        var pathname = location.pathname
        pathname = pathname.substring(1,pathname.length);
        pathname = pathname.substring(0,pathname.indexOf('/'));
        return '/'+pathname;
    },

    getWSApiPath : function() {
        var wp = 'ws://' + location.host+ + jmicro.config.wsContext;
        return wp;
    },

    getHttpApiPath : function() {
        var wp = 'http://' + location.host + jmicro.config.httpContext;
        return wp;
    },

    getWebResourceHttpPath : function(subPath) {
        var wp = 'http://' + location.host + jmicro.utils.getWebContextPath()
            + subPath;
        return wp;
    }
}

jmicro.utils = {
    _genId : 0,
    getId: function(){
        return this._genId ++;
    },

    getTimeAsMills: function() {
        return new Date().getTime();
    },

    strByteLength:  function(str)  {
        var i;
        var len;
        len = 0;
        for (i=0;i<str.length;i++)  {
            if (str.charCodeAt(i)>255) len+=2; else len++;
        }
        return len;
    },

    isInteger: function (value)  {
        if ('/^(\+|-)?\d+$/'.test(value )){
            return true;
        }else {
            return false;
        }
    },

    isFloat: function(value){
        if ('/^(\+|-)?\d+($|\.\d+$)/'.test(value )){
            return true;
        }else{
            return false;
        }
    } ,

    checkUrl: function (value){
        var myReg = '/^((http:[/][/])?\w+([.]\w+|[/]\w*)*)?$/';
        return myReg.test( value );
    },

    checkEmail: function (value){
        var myReg = /^([-_A-Za-z0-9\.]+)@([_A-Za-z0-9]+\.)+[A-Za-z0-9]{2,3}$/;
        return myReg.test(value);
    },

    checkIP:   function (value)   {
        var re='/^(\d+)\.(\d+)\.(\d+)\.(\d+)$/';
        if(re.test( value ))  {
            if( RegExp.$1 <256 && RegExp.$2<256 && RegExp.$3<256 && RegExp.$4<256)
                return true;
        }
        return false;
    },

    inherits : function(child, parentCtor) {
        function tempCtor() {};
        tempCtor.prototype = parentCtor.prototype;
        child.superClass_ = parentCtor.prototype;
        child.prototype = new tempCtor();
        child.prototype.constructor = child;
    },

    bind : function(scope, funct){
        return function(){
            return funct.apply(scope, arguments);
        };
    },

    appendUrlParams:function(url,params) {
        if(params) {
            url = url + '?';
            for(var p in params) {
                url = url + p + '=' + params[p]+'&';
            }
            url = url.substr(0, url.length - 1);
        }
        return url;
    },

    parseUrlParams:function(url1) {
        var url = url1;
        if(!url) {
            url = window.location.href;
        }
        var index = url.indexOf('?');

        if(index < 0) {
            return {};
        }

        url = decodeURI(url);
        var qs = url.substring(index+1);

        var params = {};
        var arr = qs.split('&');
        for(var i =0; i < arr.length; i++) {
            var kv = arr[i].split('=');
            params[kv[0]]=kv[1];
        }

        return params;
    },

    isBrowser : function(browser) {
        return jmicro.utils.browser[browser] != null;
    },

    version : function() {
        for(var b in jmicro.utils.browser) {
            return jmicro.utils.browser[b][1].split('.')[0];
        }
    },

    isIe : function() {
        return this.isBrowser(jmicro.utils.Constants.IE9) ||
            this.isBrowser(jmicro.utils.Constants.IE8) ||
            this.isBrowser(jmicro.utils.Constants.IE7) ||
            this.isBrowser(jmicro.utils.Constants.IE6)
    },

    toJson : function(obj) {
        if(typeof obj === 'string') {
            return obj;
        } else if(typeof obj === 'object') {
            return JSON.stringify(obj);
        }else {
            throw 'obj cannot transfor to Json'
        }
    },

    fromJson : function(jsonStr) {
        console.log(typeof jsonStr);
        if(typeof jsonStr === 'string') {
            var msg = eval('('+jsonStr+')');
            if(msg.status){
                msg.status = eval('('+msg.status+')');
            }
            return new jmicro.Message(msg);
        }else if(typeof jsonStr === 'object') {
            return jsonStr;
        } else  {
            throw 'fail from Json: ' + jsonStr;
        }

    },

    fromJ : function(jsonStr) {
        if(typeof jsonStr === 'string') {
            var msg = eval('('+jsonStr+')');
            if(msg.status){
                msg.status = eval('('+msg.status+')');
            }
            return msg;
        }else if(typeof jsonStr === 'object') {
            return jsonStr;
        } else  {
            throw 'fail from Json: ' + jsonStr;
        }

    },

    clone : function(jsObj) {
        var type = typeof jsObj;
        if(type != 'object') {
            return jsObj;
        }
        var obj = {};
        for(var i in jsObj) {
            var o = jsObj[i];
            obj[i] = jmicro.utils.clone(o);
        }
        return obj;
    },

    isLoad: function(jsUrl) {
        var scripts = document.getElementsByTagName('script');
        if(!scripts || scripts.length < 1) {
            return false;
        }
        for(var i = 0; i < scripts.length; i++) {
            if(jsUrl == scripts[i].src ) {
                return true;
            }
        }
        return false;
    },

    load: function(jsUrl) {
        if(this.isLoad(jsUrl)) {
            return ;
        }
        var oHead = document.getElementsByTagName('HEAD').item(0);
        var oScript= document.createElement("script");
        oScript.type = "text/javascript";
        oScript.src=jsUrl;
        oHead.appendChild( oScript);
    }

}

jmicro.localStorage = new function() {
    //this.updateBrowser_ = jmicro.utils.i18n.get('update_your_browser');
    this.set = function(key,value) {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        localStorage.setItem(key,value);
    }

    this.remove = function(key) {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        localStorage.removeItem(key);
    }

    this.get = function(key) {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        return localStorage.getItem(key);
    }

    this.clear = function() {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        if(confirm('this operation will clear all local storage data?')) {
            localStorage.clear();
        }
    }

    this.supportLocalstorage = function() {
        try {
            return window.localStorage !== null;
        } catch (e) {
            return false;
        }
    }

    this.isSupport = function() {
        if(jmicro.utils.isIe() && jmicro.utils.version() < 8) {
            return false;
        }else {
            return true;
        }
    }

}

jmicro.sessionStorage = new function() {
    //this.updateBrowser_ = jmicro.utils.i18n.get('update_your_browser');
    this.set = function(key,value) {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        sessionStorage.setItem(key,value);
    }

    this.remove = function(key) {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        sessionStorage.removeItem(key);
    }

    this.get = function(key) {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        return sessionStorage.getItem(key);
    }

    this.clear = function() {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        sessionStorage.clear();
    }

    this.supportLocalstorage = function() {
        try {
            return window.sessionStorage !== null;
        } catch (e) {
            return false;
        }
    }
}

jmicro.utils.Constants={
    // debug level
    INFO:'INFO',
    DEBUG:'DEBUG',
    ERROR:'ERROR',
    FINAL:'FINAL',
    DEFAULT:'DEFAULT',
    IE:'ie',
    IE6:'ie6',
    IE7:'ie7',
    IE8:'ie8',
    IE9:'ie9',
    IE10:'ie10',
    IE10:'ie11',
    chrome:'chrome',
    firefox:'firefox',
    safari:'safari',
    opera:'opera'
}

if(!jmicro.utils.browser) {
    var ua = navigator.userAgent.toLowerCase();
    var s = null;
    var key = null;
    var bv = null;
    //"mozilla/5.0 (windows nt 10.0; wow64; trident/7.0; .net4.0c; .net4.0e; manmjs; rv:11.0) like gecko"
    if(s = ua.match(/msie ([\d.]+)/)) {
        key = jmicro.utils.Constants.IE;
        key = key + s[1].split('.')[0];
    }else if(s = ua.match(/rv\:([\d.]+)/)) {
        key = jmicro.utils.Constants.IE
    }else if(s = ua.match(/firefox\/([\d.]+)/)) {
        key = jmicro.utils.Constants.firefox
    }else if(s = ua.match(/chrome\/([\d.]+)/)) {
        key = jmicro.utils.Constants.chrome;
    }else if(s = ua.match(/opera.([\d.]+)/)) {
        key = jmicro.utils.Constants.opera;
    }else if(s = ua.match(/version\/([\d.]+).*safari/)) {
        key = jmicro.utils.Constants.safari;
    }
    jmicro.utils.browser = {};
    if(s != null) {
        jmicro.utils.browser[key] = [];
        jmicro.utils.browser[key][0] = $.trim(s[0]);
        jmicro.utils.browser[key][1] = $.trim(s[1]);
    }
}

if(jmicro.utils.isBrowser('ie')) {
    Array.prototype.fill = function(e) {
        for(var i = 0; i < this.length; i++) {
            this[i] = e;
        }
    }
}


