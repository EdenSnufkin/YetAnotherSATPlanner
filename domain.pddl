;; logistics domain Typed version.
;;

(define (domain logistics)
  (:requirements :strips :typing) 
  (:types physobj
          place)
  
  (:predicates (at ?obj - physobj ?loc - place))

(:action MOVE-OBJ
   :parameters (?obj - physobj ?loc-from - place ?loc-to - place)
   :precondition
    (at ?obj ?loc-from)
   :effect
    (and (not (at ?obj ?loc-from)) (at ?obj ?loc-to)))
)