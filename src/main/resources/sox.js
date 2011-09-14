(function($){
  $(function(){
    $("html").attr({"data-useragent":  navigator.userAgent, "data-platform": navigator.platform });
    //$(document).bind('keyup', function(e){
    //  if((e.keyCode || e.which) == 91/*c*/) { $("div.content").toggle(); }
    //});
    $("h1 a").bind('click', function(e){
      $(this).find("div.content").toggle();
    });
  });
})(jQuery);