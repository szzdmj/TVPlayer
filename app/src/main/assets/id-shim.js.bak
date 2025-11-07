/*
 * Android WebView id 映射补丁（简化版）
 * - 初始扫描所有带 id 的元素，挂到 window 同名属性上；
 * - 监听 DOM 变化，新增节点自动挂载；
 * - onerror 钩子：捕获 “XXX is not defined” 时尝试用 getter 补齐。
 */
(function(){
  if (window.__ID_SHIM_INSTALLED__) return;
  function expose(el){
    var id = el && el.id;
    if (!id) return;
    if (id in window) return;
    try {
      Object.defineProperty(window, id, {
        configurable: true,
        enumerable: false,
        get: function(){ return document.getElementById(id); }
      });
    } catch(e){
      try { window[id] = document.getElementById(id); } catch(_){}
    }
  }
  function scan(root){
    try{
      var list = (root||document).querySelectorAll('[id]');
      for (var i=0;i<list.length;i++) expose(list[i]);
    }catch(_){}
  }
  scan(document);
  try {
    new MutationObserver(function(muts){
      for (var i=0;i<muts.length;i++){
        var nodes = muts[i].addedNodes||[];
        for (var j=0;j<nodes.length;j++){
          var n = nodes[j];
          if (n && n.nodeType===1){
            expose(n);
            try{
              var inner = n.querySelectorAll ? n.querySelectorAll('[id]') : [];
              for (var k=0;k<inner.length;k++) expose(inner[k]);
            }catch(_){}
          }
        }
      }
    }).observe(document.documentElement, {subtree:true, childList:true});
  } catch(_){}
  window.addEventListener('error', function(e){
    try{
      var m = e && e.message ? String(e.message) : '';
      var m2 = /([^ ]+) is not defined/.exec(m);
      if (m2 && m2[1]){
        var name = m2[1];
        if (!(name in window)) {
          try {
            Object.defineProperty(window, name, {
              configurable:true,
              enumerable:false,
              get:function(){ return document.getElementById(name); }
            });
          } catch(_){}
        }
      }
    }catch(_){}
  }, true);
  window.__ID_SHIM_INSTALLED__ = true;
})();
