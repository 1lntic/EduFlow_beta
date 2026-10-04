const vm=require('vm');const assert=require('assert');
const source=require('fs').readFileSync(require('path').join(__dirname,'../app/src/main/assets/journal-api.js'),'utf8');
const student={aktywny:true,key:'student-secret-key',uczen:'Uczeń testowy',oddzial:'4T',jednostka:'Szkoła testowa',idDziennik:7};
async function run({op='student',overrides={},cfg={},loc='/test/start'}={}){
  const calls=[];
  const payloads={Context:{uczniowie:[student]},OcenyTablica:[{przedmiot:'Matematyka',ocena:'5',data:'2026-10-04',kategoria:'Test',waga:2,komentarz:'<b>OK</b>'}],PlanZajec:[{przedmiot:'Programowanie',godzinaOd:'10:45',godzinaDo:'11:30',prowadzacy:'Nauczyciel testowy',sala:'204'}]};
  const c={job:'ef_'+('a'.repeat(32)),base:'https://uczen.eduvulcan.pl/test',operation:op,studentKey:student.key,journalId:'7',date:'2026-10-04',from:'2026-10-03T22:00:00Z',to:'2026-10-04T21:59:59.999Z',...cfg};
  const context={window:{},location:{protocol:'https:',hostname:'uczen.eduvulcan.pl',origin:'https://uczen.eduvulcan.pl',pathname:loc},URL,AbortController,setTimeout,clearTimeout};
  context.fetch=async(url,options)=>{calls.push({url,options});const endpoint=new URL(url).pathname.split('/').pop();const override=overrides[endpoint]||{};return{ok:override.status?false:true,status:override.status||200,headers:{get:()=>override.type||'application/json'},text:async()=>override.raw!==undefined?override.raw:JSON.stringify(override.data!==undefined?override.data:payloads[endpoint])};};
  vm.createContext(context);vm.runInContext(source.replace('__CONFIG__',JSON.stringify(c)),context);
  const limit=Date.now()+2000;
  while(context.window[c.job].result.status==='pending'&&Date.now()<limit)await new Promise(r=>setTimeout(r,2));
  assert.notEqual(context.window[c.job].result.status,'pending');
  return {result:JSON.parse(JSON.stringify(context.window[c.job].result)),calls};
}
(async()=>{
  const tests=[];
  let r=await run({op:'context'});assert.equal(r.result.status,'done');assert.equal(r.result.data.students[0].name,'Uczeń testowy');tests.push('Context i profile');
  r=await run();assert.equal(r.result.status,'done');assert.equal(r.result.data.grades[0].grade,'5');assert.equal(r.result.data.grades[0].comment,'OK');assert.equal(r.result.data.lessons[0].date,'2026-10-04');assert(!JSON.stringify(r.result.data).includes(student.key));tests.push('Normalizacja ocen i planu, bez klucza ucznia w migawce');
  assert(r.calls.every(c=>c.options.credentials==='same-origin'&&c.options.redirect==='error'&&!c.options.headers.Cookie));tests.push('Same-origin i blokowanie przekierowań');
  assert.equal(new URL(r.calls.find(c=>c.url.includes('PlanZajec')).url).searchParams.get('dataOd'),'2026-10-03T22:00:00Z');tests.push('Kodowanie zakresu dat');
  r=await run({cfg:{studentKey:'inny-klucz'}});assert.equal(r.result.error,'STUDENT_CONTEXT_CHANGED');assert.equal(r.calls.length,1);tests.push('Weryfikacja profilu przy każdym pobraniu');
  r=await run({overrides:{OcenyTablica:{status:403}}});assert.equal(r.result.error,'HTTP_403');tests.push('HTTP 403 nie zwraca pustego sukcesu');
  r=await run({overrides:{Context:{type:'text/html',raw:'<form>login</form>'}}});assert.equal(r.result.error,'NOT_JSON_LOGIN_OR_SCHEMA');tests.push('Odrzucenie formularza logowania');
  r=await run({overrides:{OcenyTablica:{data:{items:[]}}}});assert(r.result.error.startsWith('SCHEMA_GRADES'));tests.push('Odrzucenie nieznanego schematu');
  r=await run({overrides:{OcenyTablica:{data:[{przedmiot:'Matematyka',ocena:{value:'5'}}]}}});assert(r.result.error.startsWith('SCHEMA_'));tests.push('Odrzucenie zagnieżdżonej wartości oceny');
  r=await run({overrides:{OcenyTablica:{data:[]},PlanZajec:{data:[]}}});assert.equal(r.result.status,'done');assert.equal(r.result.data.lessons.length,0);tests.push('Prawidłowe puste listy');
  r=await run({loc:'/innaregion/start'});assert.equal(r.result.error,'WRONG_PORTAL');assert.equal(r.calls.length,0);tests.push('Blokowanie innego regionu');
  r=await run({overrides:{Context:{raw:'{bad'}}});assert.equal(r.result.error,'INVALID_JSON');tests.push('Odrzucenie uszkodzonego JSON');
  console.log(JSON.stringify(tests));
})().catch(e=>{console.error(e);process.exitCode=1;});
