(function (cfg) {
  const root = document.documentElement;
  if (!root) return;
  const state = window.__eduFlow || (window.__eduFlow = {});
  state.cfg = cfg;
  function mount() {
    const c = state.cfg;
    if (!c.enabled) {
      root.removeAttribute('data-eduflow');
      root.removeAttribute('data-ef-compact');
      ['eduflow-style','eduflow-custom'].forEach(id => document.getElementById(id)?.remove());
      return;
    }
    root.dataset.eduflow = c.theme;
    root.dataset.efCompact = String(c.compact);
    [['eduflow-style',c.css],['eduflow-custom',c.customCss]].forEach(([id,css]) => {
      let el = document.getElementById(id);
      if (!el) { el = document.createElement('style'); el.id = id; (document.head || root).appendChild(el); }
      if (el.textContent !== css) el.textContent = css;
    });
  }
  mount();
  if (!state.observer) {
    let queued = false;
    state.observer = new MutationObserver(() => {
      if (queued) return;
      queued = true;
      requestAnimationFrame(() => { queued = false; mount(); });
    });
    state.observer.observe(root, {childList:true,subtree:true});
  }
  return 'ok';
})(__CONFIG__);
