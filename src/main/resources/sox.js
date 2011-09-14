(function($){
  $(function(){
    $("html").attr({"data-useragent":  navigator.userAgent, "data-platform": navigator.platform });
    $("h1 a").bind('click', function(e){
      $(this).parent().siblings("div.content").toggle();
    });
  });
})(jQuery);