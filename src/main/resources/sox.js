(function($){
  $(function(){
    $("html").attr({"data-useragent":  navigator.userAgent, "data-platform": navigator.platform });
    $("h1 a").bind('click', function(e){
      $(this).parent().siblings("div.content").toggle();
    });
    $(document).bind('keyup', function(e) {
        if(e.which === 83/*s*/) {
            if(e.shiftKey) {
                $('div.content').show();
            } else {
                $('div.content').hide();
            }
        }
    });
  });
})(jQuery);