(function(){
  'use strict';
  if(window.__eduFlowSectionsMounted)return;
  window.__eduFlowSectionsMounted=true;
  const extra={achievements:'Osiągnięcia',attendance:'Frekwencja — statystyki',remarks:'Pochwały i uwagi',homework:'Sprawdziany i zadania domowe',meetings:'Zebrania'};
  Object.keys(extra).forEach(id=>supported.add(id));
  const previousDraw=draw,previousSwitch=switchPage;
  window.draw=function(){
    previousDraw();if(!Object.prototype.hasOwnProperty.call(extra,page))return;
    main.replaceChildren(el('div','eyebrow','Dane Twojego dziennika'),el('h1','',extra[page]));
    const intro=card('Zakres i aktualizacja',page==='attendance'?'Ten ekran pokazuje statystyki API. Historia pojedynczych obecności/nieobecności nie jest jeszcze podłączona.':'Dane zwrócone przez webowe API dla wybranego profilu.');
    intro.append(btn('Odśwież tę zakładkę',()=>go('section',{name:page}),'primary'));main.append(intro);
    const failures=state.sectionErrors||{};
    if(failures[page])main.append(card('Nie udało się odświeżyć',String(failures[page])+'. Poprzedni zapis pozostaje widoczny.'));
    const snapshot=state.sections&&state.sections[page];
    if(!snapshot){main.append(card('Jeszcze nie pobrano danych','Brak zapisu lokalnego nie oznacza pustej listy w dzienniku.'));return;}
    main.append(card('Zapis: '+stamp(snapshot.fetchedAt),snapshot.scope||''));
    if(snapshot.from&&snapshot.to)main.append(card('Zakres dat',stamp(snapshot.from)+' — '+stamp(snapshot.to)));
    const items=Array.isArray(snapshot.records)?snapshot.records:[];
    if(!items.length){main.append(card('Serwer zwrócił pustą listę','Dotyczy podanego zakresu i ostatniej udanej aktualizacji.'));return;}
    for(const record of items){
      const c=card(record.title||'Pozycja','');
      for(const field of Array.isArray(record.fields)?record.fields:[]){
        const row=el('div');row.style.marginBottom='10px';row.append(el('div','mini',field.label||'Pole'),el('div','body-text',field.value==null?'':field.value));c.append(row);
      }
      main.append(c);
    }
  };
  window.switchPage=function(id){previousSwitch(id);if(Object.prototype.hasOwnProperty.call(extra,id))go('section',{name:id});};
  draw();
})();
