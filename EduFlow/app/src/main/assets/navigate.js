(function (section) {
  const names = {
    grades:['oceny','oceny ucznia','oceny bieżące'],
    timetable:['plan lekcji','plan zajęć'],
    messages:['wiadomości','wiadomości ze szkoły'],
    attendance:['frekwencja','obecności','nieobecności']
  };
  const normalize = s => (s || '').replace(/\s+/g,' ').trim().toLocaleLowerCase('pl');
  const wanted = names[section] || [];
  const candidates = [...document.querySelectorAll('a,button,[role="tab"],[role="menuitem"]')];
  const match = candidates.find(el => {
    const label = normalize(el.getAttribute('aria-label') || el.textContent);
    const r = el.getBoundingClientRect();
    const cs = getComputedStyle(el);
    return r.width > 0 && r.height > 0 && cs.visibility !== 'hidden' && cs.display !== 'none' &&
      wanted.some(n => label === n || label.startsWith(n + ' ('));
  });
  if (!match) return {found:false};
  match.click();
  return {found:true};
})(__SECTION__);
