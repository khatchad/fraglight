suggested(X) :- 
	enabled(Y),
	path(Y,Z,D),
	path(X,Z,E),
	not(empty(D)),
	not(empty(E)),
	distance(Y,Z,D,M),
	distance(X,Z,E,N),
	M =:= N.

distance(A,B,Path,N) :-
	nth0(J,Path,A),
	nth0(K,Path,B),
	N is abs(J-K).

connected(A,B) :-
	calls(A,B) ;
	accesses(A,B).

path(A,B,Path) :-
       travel(A,B,[A],Q), 
       reverse(Q,Path).

travel(A,B,P,[B|P]) :- 
	connected(A,B).

travel(A,B,Visited,Path) :-
       connected(A,C),           
       C \== B,
       \+member(C,Visited),
       travel(C,B,[C|Visited],Path).  

pathExists(A,B) :-
	path(A,B,P), 
	not(empty(P)).

empty([]) :- 
	true.

reverse(List, Reversed) :-
	reverse(List, [], Reversed).

reverse([], Reversed, Reversed).

reverse([Head|Tail], SoFar, Reversed) :-
	reverse(Tail, [Head|SoFar], Reversed).

member(X,[X|_]).

member(X,[_|T]) :- 
	member(X,T).