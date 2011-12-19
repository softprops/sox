(function($){
  $(function(){
    $("html").attr({"data-useragent":  navigator.userAgent, "data-platform": navigator.platform });
    $("h1 a").bind('click', function(e){
      $(this).parent().siblings("div.content").toggle();
    });
    $(document).bind('keyup', function(e) {
        switch(e.which) {
        case 83 /*3*/:
            if(e.shiftKey) {
                $('div.content').show();
            } else {
                $('div.content').hide();
            }
            break;
        case 70/*f*/:
            $("#find").slideToggle('fast');
        }
    });
  });
})(jQuery);